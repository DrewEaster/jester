package com.dreweaster.jester.application.repository.deduplicating;

import com.dreweaster.jester.application.eventstore.EventStore;
import com.dreweaster.jester.application.eventstore.PersistedEvent;
import com.dreweaster.jester.domain.*;
import javaslang.concurrent.Future;
import javaslang.concurrent.Promise;
import javaslang.control.Either;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 */
public abstract class CommandDeduplicatingEventsourcedAggregateRepository<A extends Aggregate<C, E, State>, C extends DomainCommand, E extends DomainEvent, State> implements AggregateRepository<A, C, E, State> {

    private Class<A> aggregateType;

    private static final Logger LOGGER = LoggerFactory.getLogger(CommandDeduplicatingEventsourcedAggregateRepository.class);

    private EventStore eventStore;

    private CommandDeduplicationStrategyFactory commandDeduplicationStrategyFactory;

    public CommandDeduplicatingEventsourcedAggregateRepository(
            Class<A> aggregateType,
            EventStore eventStore,
            CommandDeduplicationStrategyFactory commandDeduplicationStrategyFactory) {
        this.aggregateType = aggregateType;
        this.eventStore = eventStore;
        this.commandDeduplicationStrategyFactory = commandDeduplicationStrategyFactory;
    }

    @Override
    public final AggregateRoot<C, E> aggregateRootOf(AggregateId aggregateId) {
        return (commandId, command) -> new DeduplicatingCommandHandler(aggregateType).handle(
                CommandEnvelope.of(
                        aggregateId,
                        commandId,
                        command)
        ).map(events -> events.stream().map(PersistedEvent::rawEvent).collect(Collectors.toList()));
    }

    // TODO: Snapshots will have to store last n minutes/hours/days of command ids within their payload.
    private class DeduplicatingCommandHandler {

        private Class<A> aggregateType;

        public DeduplicatingCommandHandler(Class<A> aggregateType) {
            this.aggregateType = aggregateType;
        }

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

    public static class CommandEnvelope<T> {

        public static <T> CommandEnvelope<T> of(AggregateId aggregateId, CommandId id, T payload) {
            return new CommandEnvelope<T>(aggregateId, id, payload) {
                @Override
                public AggregateId aggregateId() {
                    return super.aggregateId();
                }

                @Override
                public CommandId id() {
                    return super.id();
                }
            };
        }

        public static <T> CommandEnvelope<T> of(AggregateId aggregateId, T payload) {
            return new CommandEnvelope<T>(aggregateId, CommandId.of(UUID.randomUUID().toString()), payload) {
                @Override
                public AggregateId aggregateId() {
                    return super.aggregateId();
                }

                @Override
                public CommandId id() {
                    return super.id();
                }
            };
        }

        private AggregateId aggregateId;

        private CommandId id;

        private T payload;

        public CommandEnvelope(AggregateId aggregateId, CommandId id, T payload) {
            this.id = id;
            this.aggregateId = aggregateId;
            this.payload = payload;
        }

        public AggregateId aggregateId() {
            return aggregateId;
        }

        public CommandId id() {
            return id;
        }

        public T payload() {
            return payload;
        }

        @Override
        public String toString() {
            return "CommandEnvelope{" +
                    "aggregateId=" + aggregateId +
                    ", id=" + id +
                    ", payload=" + payload +
                    '}';
        }
    }
}
