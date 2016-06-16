package com.dreweaster.ddd.framework;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

/**
 */
public class DeduplicatingCommandHandlerFactory implements CommandHandlerFactory {

    private AggregateRootFactory aggregateRootFactory;

    private EventStore eventStore;

    private CommandDeduplicationStrategyFactory commandDeduplicationStrategyFactory;

    public DeduplicatingCommandHandlerFactory(AggregateRootFactory aggregateRootFactory,
                                              EventStore eventStore,
                                              CommandDeduplicationStrategyFactory commandDeduplicationStrategyFactory) {

        this.aggregateRootFactory = aggregateRootFactory;
        this.eventStore = eventStore;
        this.commandDeduplicationStrategyFactory = commandDeduplicationStrategyFactory;
    }

    @Override
    public <A extends Aggregate<C, E, State>, C extends DomainCommand, E extends DomainEvent, State> CommandHandler<A, C, E, State> handlerFor(Class<A> aggregateType) {
        return new DeduplicatingCommandHandler<>(aggregateType);
    }

    // TODO: Snapshots will have to store last n minutes/hours/days of command ids within their payload.
    private class DeduplicatingCommandHandler<A extends Aggregate<C, E, State>, C extends DomainCommand, E extends DomainEvent, State> implements CommandHandler<A, C, E, State> {

        private Class<A> aggregateType;

        public DeduplicatingCommandHandler(Class<A> aggregateType) {
            this.aggregateType = aggregateType;
        }

        @Override
        public CompletionStage<List<PersistedEvent<A, E>>> handle(CommandEnvelope<? extends C> command) {
            return eventStore.loadEvents(aggregateType, command.aggregateId()).thenCompose(previousEvents -> {
                Long expectedSequenceNumber = -1L;

                CommandDeduplicationStrategyBuilder commandDeduplicationStrategyBuilder =
                        commandDeduplicationStrategyFactory.newBuilder();

                List<E> rawPreviousEvents = new ArrayList<>();
                for (PersistedEvent<A, E> persistedEvent : previousEvents) {
                    rawPreviousEvents.add(persistedEvent.rawEvent());

                    // Only command ids that the strategy opts in will be used to detect duplicates. A strategy will
                    // typically only treat command ids as candidates for comparison for commands tied to events that
                    // were generated within the time window for which duplicates are theoretically possible. For example,
                    // when using a message layer like Kafka or Kinesis, the messaging system may only store 24 hours
                    // of messages, thus it wouldn't be possible to receive duplicates beyond a 24 hour period. In
                    // practice it's actually most likely that duplicates would be found in much smaller time
                    // windows. The decision on strategy really needs to be made on a use case by use case basis.
                    // For some contexts, it might be ok to assume duplicates within a narrow window (e.g. a few minutes)
                    // because the outcome of incorrectly processing a duplicate might not really be that problematic.
                    commandDeduplicationStrategyBuilder.addEvent(persistedEvent);

                    expectedSequenceNumber = persistedEvent.sequenceNumber();
                }

                CommandDeduplicationStrategy deduplicationStrategy = commandDeduplicationStrategyBuilder.build();

                AggregateRootRef<C, E> aggregateRootRef = aggregateRootFactory.aggregateOf(
                        aggregateType,
                        command.aggregateId(),
                        rawPreviousEvents);

                // First we check whether we've handled this command before
                // If caller is using deterministic command ids, this essentially allows idempotent command handling
                // Note it really is down to the caller because only the caller can possible make the decision about how
                // to generate deterministic command ids. We simply provide the guarantee that we'll ignore any command ids
                // we've seen that is not filtered out by the deduplication strategy.
                if (!deduplicationStrategy.isDuplicate(command.id())) {
                    final Long finalExpectedSequenceNumber = expectedSequenceNumber;

                    return aggregateRootRef.handle(command.payload()).thenCompose(generatedEvents -> eventStore.saveEvents(
                            aggregateType,
                            command.aggregateId(),
                            command.id(),
                            generatedEvents,
                            finalExpectedSequenceNumber));
                } else {
                    // TODO: We should capture metrics about duplicated commands
                    // TODO: Capture/log/report on age of duplicate commands
                    return CompletableFuture.completedFuture(Collections.emptyList());
                }
            });
        }
    }
}
