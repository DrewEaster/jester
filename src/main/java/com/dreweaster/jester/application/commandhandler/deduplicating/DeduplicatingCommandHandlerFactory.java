package com.dreweaster.jester.application.commandhandler.deduplicating;

import com.dreweaster.jester.application.commandhandler.*;
import com.dreweaster.jester.application.eventstore.EventStore;
import com.dreweaster.jester.application.eventstore.PersistedEvent;
import com.dreweaster.jester.domain.Aggregate;
import com.dreweaster.jester.domain.AggregateId;
import com.dreweaster.jester.domain.Behaviour;
import com.dreweaster.jester.domain.CommandContext;
import com.dreweaster.jester.domain.DomainCommand;
import com.dreweaster.jester.domain.DomainEvent;
import javaslang.concurrent.Future;
import javaslang.concurrent.Promise;
import javaslang.control.Either;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 */
public class DeduplicatingCommandHandlerFactory implements CommandHandlerFactory {

    private static final Logger LOGGER = LoggerFactory.getLogger(DeduplicatingCommandHandlerFactory.class);

    private EventStore eventStore;

    private CommandDeduplicationStrategyFactory commandDeduplicationStrategyFactory;

    public DeduplicatingCommandHandlerFactory(EventStore eventStore,
                                              CommandDeduplicationStrategyFactory commandDeduplicationStrategyFactory) {

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
        public Future<List<PersistedEvent<A, E>>> handle(CommandEnvelope<? extends C> command) {
            // TODO: Load snapshot first (if any)
            return eventStore.loadEvents(aggregateType, command.aggregateId()).flatMap(previousEvents -> {
                Long expectedSequenceNumber = -1L;

                CommandDeduplicationStrategyBuilder commandDeduplicationStrategyBuilder =
                        commandDeduplicationStrategyFactory.newBuilder();

                List<E> rawPreviousEvents = new ArrayList<>();
                for (PersistedEvent<A, E> persistedEvent : previousEvents) {
                    rawPreviousEvents.add(persistedEvent.rawEvent());
                    commandDeduplicationStrategyBuilder.addEvent(persistedEvent);
                    expectedSequenceNumber = persistedEvent.sequenceNumber();
                }

                CommandDeduplicationStrategy deduplicationStrategy = commandDeduplicationStrategyBuilder.build();

                AggregateRootRef<A, C, E, State> aggregateRootRef = new AggregateRootRef<>(
                        aggregateType,
                        command.aggregateId(),
                        rawPreviousEvents);

                if (!deduplicationStrategy.isDuplicate(command.id())) {
                    final Long finalExpectedSequenceNumber = expectedSequenceNumber;

                    return aggregateRootRef.handle(command.payload()).flatMap(generatedEvents -> eventStore.saveEvents(
                            aggregateType,
                            command.aggregateId(),
                            command.id(),
                            generatedEvents,
                            finalExpectedSequenceNumber));
                } else {
                    // TODO: We should capture metrics about duplicated commands
                    // TODO: Capture/log/report on age of duplicate commands
                    LOGGER.info("Skipped processing duplicate command: " + command);
                    return Future.successful(Collections.emptyList());
                }
            });
        }
    }

    private class AggregateRootRef<A extends Aggregate<C, E, State>, C extends DomainCommand, E extends DomainEvent, State> {

        private Class<A> aggregateType;

        private AggregateId aggregateId;

        private List<E> previousEvents;

        public AggregateRootRef(Class<A> aggregateType, AggregateId aggregateId, List<E> previousEvents) {
            this.aggregateType = aggregateType;
            this.aggregateId = aggregateId;
            this.previousEvents = previousEvents;
        }

        public Future<List<E>> handle(C command) {
            Promise<List<E>> promise = Promise.make();

            try {
                A aggregateInstance = aggregateType.newInstance();

                // TODO: Pass snapshot once implemented
                Behaviour<C, E, State> behaviour = aggregateInstance.initialBehaviour();

                for (E event : previousEvents) {
                    behaviour = behaviour.handleEvent(event);
                }

                final Behaviour<C, E, State> finalBehaviour = behaviour;

                Either<Throwable, List<E>> handled = behaviour.handleCommand(command, new CommandContext<E, State>() {

                    @Override
                    public State currentState() {
                        return finalBehaviour.state();
                    }

                    @Override
                    public AggregateId aggregateId() {
                        return aggregateId;
                    }
                });

                handled.bimap(promise::failure, promise::success);

            } catch (Exception ex) {
                // TODO: Do we need to handle this more specifically? Caused by aggregate instance creation failure
                promise.failure(ex);
            }

            return promise.future();
        }
    }

}
