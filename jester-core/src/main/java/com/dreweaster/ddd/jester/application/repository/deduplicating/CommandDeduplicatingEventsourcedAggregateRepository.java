package com.dreweaster.ddd.jester.application.repository.deduplicating;

import com.dreweaster.ddd.jester.application.eventstore.EventStore;
import com.dreweaster.ddd.jester.application.eventstore.PersistedEvent;
import com.dreweaster.ddd.jester.domain.*;
import javaslang.Tuple2;
import javaslang.Tuple3;
import javaslang.collection.List;
import javaslang.concurrent.Future;
import javaslang.concurrent.Promise;
import javaslang.control.Either;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

/**
 */
public abstract class CommandDeduplicatingEventsourcedAggregateRepository<A extends Aggregate<C, E, State>, C extends DomainCommand, E extends DomainEvent, State> implements AggregateRepository<A, C, E, State> {

    private AggregateType<A, C, E, State> aggregateType;

    private static final Logger LOGGER = LoggerFactory.getLogger(CommandDeduplicatingEventsourcedAggregateRepository.class);

    private EventStore eventStore;

    private CommandDeduplicationStrategyFactory commandDeduplicationStrategyFactory;

    public CommandDeduplicatingEventsourcedAggregateRepository(
            AggregateType<A, C, E, State> aggregateType,
            EventStore eventStore,
            CommandDeduplicationStrategyFactory commandDeduplicationStrategyFactory) {
        this.aggregateType = aggregateType;
        this.eventStore = eventStore;
        this.commandDeduplicationStrategyFactory = commandDeduplicationStrategyFactory;
    }

    @Override
    public final AggregateRoot<C, E, State> aggregateRootOf(AggregateId aggregateId) {
        return new DeduplicatingCommandHandler(aggregateId, aggregateType);
    }

    // TODO: Snapshots will have to store last n minutes/hours/days of command ids within their payload.
    private class DeduplicatingCommandHandler implements AggregateRoot<C, E, State> {

        private AggregateType<A, C, E, State> aggregateType;

        private AggregateId aggregateId;

        public DeduplicatingCommandHandler(AggregateId aggregateId, AggregateType<A, C, E, State> aggregateType) {
            this.aggregateId = aggregateId;
            this.aggregateType = aggregateType;
        }

        @Override
        public Future<Optional<State>> state() {
            return eventStore.loadEvents(aggregateType, aggregateId).flatMap(previousEvents -> new AggregateRootRef<>(
                    aggregateType,
                    aggregateId,
                    previousEvents.map(PersistedEvent::rawEvent)).state());
        }

        @Override
        public Future<List<? super E>> handle(CommandEnvelope<C> commandEnvelope) {
            return doHandle(AggregateRoutingCommandEnvelopeWrapper.of(aggregateId, commandEnvelope)).map(events ->
                    events.map(PersistedEvent::rawEvent));
        }

        private Future<List<PersistedEvent<A, E>>> doHandle(AggregateRoutingCommandEnvelopeWrapper<? extends C> wrapper) {
            // TODO: Load snapshot first (if any)
            return eventStore.loadEvents(aggregateType, wrapper.aggregateId()).flatMap(previousEvents -> {
                Tuple3<Long, List<E>, CommandDeduplicationStrategyBuilder> tuple = previousEvents.foldLeft(
                        new Tuple3<Long, List<E>, CommandDeduplicationStrategyBuilder>(
                                -1L, List.empty(), commandDeduplicationStrategyFactory.newBuilder()), (acc, e) ->
                                new Tuple3<>(e.sequenceNumber(), acc._2.append(e.rawEvent()), acc._3.addEvent(e)));

                CommandDeduplicationStrategy deduplicationStrategy = tuple._3.build();

                AggregateRootRef<A, C, E, State> aggregateRootRef = new AggregateRootRef<>(
                        aggregateType,
                        wrapper.aggregateId(),
                        tuple._2);

                if (!deduplicationStrategy.isDuplicate(wrapper.commandEnvelope.commandId())) {
                    final Long finalExpectedSequenceNumber = tuple._1;

                    // TODO: Allow state serialisation to be configurable
                    return wrapper.commandEnvelope().correlationId().map(correlationId ->
                                    aggregateRootRef.handle(wrapper.commandEnvelope.command()).flatMap(generatedEventsAndState ->
                                            eventStore.saveEventsAndState(
                                                    aggregateType,
                                                    wrapper.aggregateId(),
                                                    CausationId.of(wrapper.commandEnvelope().commandId().get()),
                                                    correlationId,
                                                    generatedEventsAndState._1,
                                                    generatedEventsAndState._2,
                                                    finalExpectedSequenceNumber))
                    ).getOrElse(aggregateRootRef.handle(wrapper.commandEnvelope.command()).flatMap(generatedEventsAndState ->
                            eventStore.saveEventsAndState(
                                    aggregateType,
                                    wrapper.aggregateId(),
                                    CausationId.of(wrapper.commandEnvelope().commandId().get()),
                                    generatedEventsAndState._1,
                                    generatedEventsAndState._2,
                                    finalExpectedSequenceNumber)));
                } else {
                    // TODO: We should capture metrics about duplicated commands
                    // TODO: Capture/log/report on age of duplicate commands
                    LOGGER.info("Skipped processing duplicate command: " + wrapper.commandEnvelope().command());
                    return Future.successful(List.empty());
                }
            });
        }
    }

    private class AggregateRootRef<A extends Aggregate<C, E, State>, C extends DomainCommand, E extends DomainEvent, State> {

        private AggregateType<A, C, E, State> aggregateType;

        private AggregateId aggregateId;

        private List<E> previousEvents;

        public AggregateRootRef(AggregateType<A, C, E, State> aggregateType, AggregateId aggregateId, List<E> previousEvents) {
            this.aggregateType = aggregateType;
            this.aggregateId = aggregateId;
            this.previousEvents = previousEvents;
        }

        public Future<Optional<State>> state() {
            Promise<Optional<State>> promise = Promise.make();

            if(previousEvents.isEmpty()) {
                promise.success(Optional.empty());
            } else {
                try {
                    A aggregateInstance = aggregateType.clazz().newInstance();

                    // TODO: Pass snapshot once implemented
                    Behaviour<C, E, State> behaviour = aggregateInstance.initialBehaviour();

                    for (E event : previousEvents) {
                        Either<Throwable, Behaviour<C, E, State>> maybeBehaviour = behaviour.handleEvent(event);
                        if (maybeBehaviour.isLeft()) {
                            promise.failure(maybeBehaviour.getLeft());
                            return promise.future();
                        }
                        behaviour = behaviour.handleEvent(event).get();
                    }
                    promise.success(Optional.of(behaviour.state()));
                } catch (Exception ex) {
                    // TODO: Do we need to handle this more specifically? Caused by aggregate instance creation failure
                    promise.failure(ex);
                }
            }

            return promise.future();
        }

        public Future<Tuple2<List<E>,State>> handle(C command) {
            Promise<Tuple2<List<E>,State>> promise = Promise.make();

            try {
                A aggregateInstance = aggregateType.clazz().newInstance();

                // TODO: Pass snapshot once implemented
                Behaviour<C, E, State> behaviour = aggregateInstance.initialBehaviour();

                for (E event : previousEvents) {
                    Either<Throwable, Behaviour<C, E, State>> maybeBehaviour = behaviour.handleEvent(event);
                    if (maybeBehaviour.isLeft()) {
                        promise.failure(maybeBehaviour.getLeft());
                        return promise.future();
                    }
                    behaviour = behaviour.handleEvent(event).get(); // FIXME: Calling get()
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

                handled.bimap(promise::failure, eventsList -> {
                    // FIXME: Not working!
                    // Apply events to get latest state for potential serialisation
                    Behaviour<C, E, State> updatedBehaviour = eventsList.foldLeft(
                            finalBehaviour,
                            (acc,event) -> acc.handleEvent(event).get()); // FIXME: Calling get()

                    return promise.success(new Tuple2<>(eventsList, updatedBehaviour.state()));
                });

            } catch (Exception ex) {
                // TODO: Do we need to handle this more specifically? Caused by aggregate instance creation failure
                promise.failure(ex);
            }

            return promise.future();
        }
    }

    private static class AggregateRoutingCommandEnvelopeWrapper<T> {

        public static <T> AggregateRoutingCommandEnvelopeWrapper<T> of(AggregateId aggregateId, CommandEnvelope<T> commandEnvelope) {
            return new AggregateRoutingCommandEnvelopeWrapper<T>(aggregateId, commandEnvelope);
        }

        private AggregateId aggregateId;

        private CommandEnvelope<T> commandEnvelope;

        public AggregateRoutingCommandEnvelopeWrapper(AggregateId aggregateId, CommandEnvelope<T> commandEnvelope) {
            this.aggregateId = aggregateId;
            this.commandEnvelope = commandEnvelope;
        }

        public AggregateId aggregateId() {
            return aggregateId;
        }

        public CommandEnvelope<T> commandEnvelope() {
            return commandEnvelope;
        }

        @Override
        public String toString() {
            return "AggregateRoutingCommandEnvelopeWrapper{" +
                    "aggregateId=" + aggregateId +
                    ", commandEnvelope=" + commandEnvelope +
                    '}';
        }
    }
}
