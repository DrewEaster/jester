package com.dreweaster.ddd.framework;

import java.util.*;

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
    public <A, C, E> CommandHandler<A, C, E> handlerFor(Class<A> aggregateType, AggregateId aggregateId) {
        List<DomainEvent<A, E>> previousEvents = eventStore.loadEvents(aggregateType, aggregateId);
        return new DeduplicatingCommandHandler<A, C, E>(aggregateType, aggregateId, previousEvents);
    }

    @Override
    public <A, C, R> ReadOnlyCommandHandler<C, R> readOnlyHandlerFor(Class<A> aggregateType, AggregateId aggregateId) {
        // TODO: Implement this
        return null;
    }

    // TODO: Snapshots will have to store last n minutes/hours/days of command ids within their payload.
    private class DeduplicatingCommandHandler<A, C, E> implements CommandHandler<A, C, E> {

        private Class<A> aggregateType;

        private AggregateId aggregateId;

        private CommandDeduplicationStrategy deduplicationStrategy;

        private Long expectedSequenceNumber = -1L;

        private AggregateRootRef<C, E> aggregateRootRef;

        public DeduplicatingCommandHandler(Class<A> aggregateType, AggregateId aggregateId, List<DomainEvent<A, E>> previousEvents) {
            this.aggregateType = aggregateType;
            this.aggregateId = aggregateId;

            CommandDeduplicationStrategyBuilder commandDeduplicationStrategyBuilder =
                    commandDeduplicationStrategyFactory.newBuilder();

            List<E> rawPreviousEvents = new ArrayList<E>();
            for (DomainEvent<A, E> domainEvent : previousEvents) {
                rawPreviousEvents.add(domainEvent.rawEvent());

                // Only command ids that the strategy opts in will be used to detect duplicates. A strategy will
                // typically only treat command ids as candidates for comparison for commands tied to events that
                // were generated within the time window for which duplicates are theoretically possible. For example,
                // when using a message layer like Kafka or Kinesis, the messaging system may only store 24 hours
                // of messages, thus it wouldn't be possible to receive duplicates beyond a 24 hour period. In
                // practice it's actually most likely that duplicates would be found in much smaller time
                // windows. The decision on strategy really needs to be made on a use case by use case basis.
                // For some contexts, it might be ok to assume duplicates within a narrow window (e.g. a few minutes)
                // because the outcome of incorrectly processing a duplicate might not really be that problematic.
                commandDeduplicationStrategyBuilder.addDomainEvent(domainEvent);

                expectedSequenceNumber = domainEvent.sequenceNumber();
            }

            deduplicationStrategy = commandDeduplicationStrategyBuilder.build();
            aggregateRootRef = aggregateRootFactory.aggregateOf(aggregateType, aggregateId, rawPreviousEvents);
        }


        @Override
        public List<DomainEvent<A, E>> handle(Command<C> command) {
            // First we check whether we've handled this command before
            // If caller is using deterministic command ids, this essentially allows idempotent command handling
            // Note it really is down to the caller because only the caller can possible make the decision about how
            // to generate deterministic command ids. We simply provide the guarantee that we'll ignore any command ids
            // we've seen that is not filtered out by the deduplication strategy.
            if (!deduplicationStrategy.isDuplicate(command.id())) {
                List<E> generatedEvents = aggregateRootRef.handle(command.payload());
                return eventStore.saveEvents(aggregateType, aggregateId, command.id(), generatedEvents, expectedSequenceNumber);
            } else {
                // TODO: We should capture metrics about duplicated commands
                // TODO: Capture/log/report on age of duplicate commands
                return Collections.emptyList();
            }
        }
    }
}
