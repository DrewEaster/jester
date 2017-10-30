package com.dreweaster.ddd.jester.application.repository.deduplicating;

import com.dreweaster.ddd.jester.application.eventstore.EventStore;
import com.dreweaster.ddd.jester.application.eventstore.PersistedEvent;
import com.dreweaster.ddd.jester.application.repository.deduplicating.monitoring.AggregateRepositoryReporter;
import com.dreweaster.ddd.jester.application.repository.deduplicating.monitoring.CommandHandlingProbe;
import com.dreweaster.ddd.jester.domain.*;

import io.vavr.Tuple2;
import io.vavr.Tuple3;
import io.vavr.collection.List;
import io.vavr.concurrent.Future;
import io.vavr.concurrent.Promise;
import io.vavr.control.Either;
import io.vavr.control.Option;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static io.vavr.API.$;
import static io.vavr.API.Case;
import static io.vavr.Predicates.*;

import static io.vavr.API.Match;

/**
 */
public abstract class CommandDeduplicatingEventsourcedAggregateRepository<A extends Aggregate<C, E, State>, C extends DomainCommand, E extends DomainEvent, State> implements AggregateRepository<A, C, E, State> {

    private AggregateType<A, C, E, State> aggregateType;

    private static final Logger LOGGER = LoggerFactory.getLogger(CommandDeduplicatingEventsourcedAggregateRepository.class);

    private EventStore eventStore;

    private CommandDeduplicationStrategyFactory commandDeduplicationStrategyFactory;

    private List<AggregateRepositoryReporter> reporters = List.empty();

    public CommandDeduplicatingEventsourcedAggregateRepository(
            AggregateType<A, C, E, State> aggregateType,
            EventStore eventStore,
            CommandDeduplicationStrategyFactory commandDeduplicationStrategyFactory) {
        this.aggregateType = aggregateType;
        this.eventStore = eventStore;
        this.commandDeduplicationStrategyFactory = commandDeduplicationStrategyFactory;
    }

    public void addReporter(AggregateRepositoryReporter reporter) {
        reporters = reporters.append(reporter);
    }

    public void removeReporter(AggregateRepositoryReporter reporter) {
        reporters = reporters.remove(reporter);
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
        public Future<Option<State>> state() {
            return eventStore.loadEvents(aggregateType, aggregateId).flatMap(previousEvents -> new AggregateRootRef<>(
                    aggregateType,
                    aggregateId,
                    previousEvents.map(PersistedEvent::rawEvent)).state());
        }

        @Override
        public Future<CommandHandlingResult<C, E>> handle(CommandEnvelope<C> commandEnvelope) {
            AggregateRoutingCommandEnvelopeWrapper<C> wrapper = AggregateRoutingCommandEnvelopeWrapper.of(aggregateId, commandEnvelope);

            ReportingContext reportingContext = new ReportingContext(aggregateId, reporters);
            reportingContext.startedHandling(commandEnvelope);

            return doHandle(wrapper, reportingContext).recoverWith(e ->
                Match(e).of(
                    Case($(instanceOf(EventStore.OptimisticConcurrencyException.class)), () -> {
                        CommandHandlingResult<C,E> concurrentModificationResult = ConcurrentModificationResult.of(commandEnvelope);
                        reportingContext.finishedHandling(concurrentModificationResult);
                        return Future.successful(concurrentModificationResult);
                    }),
                    Case($(), () -> {
                        reportingContext.finishedHandling(e);
                        return Future.failed(e);
                    })
                )
            );
        }

        private Future<CommandHandlingResult<C, E>> doHandle(AggregateRoutingCommandEnvelopeWrapper<C> wrapper, ReportingContext reportingContext) {
            // TODO: Load snapshot first (if any)
            // TODO: When using snapshots, would need to address how this would work with deduplication algorithm
            // Snapshot would probably have to include ids of previously handled commands and their original responses
            // Alternatively could always ensure snapshot is older than the length of time strategy covers for dedupe (e.g. at least 24 hours)

            reportingContext.startedLoadingEvents();
            return eventStore.loadEvents(aggregateType, wrapper.aggregateId())
                .onFailure(reportingContext::finishedLoadingEvents)
                .flatMap(previousEvents -> {
                    reportingContext.finishedLoadingEvents(previousEvents);
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
                        Long expectedSequenceNumber = tuple._1;

                        return handleAndPersist(wrapper, aggregateRootRef, expectedSequenceNumber, reportingContext).map(maybePersistedEvents -> {
                            maybePersistedEvents.toTry().onSuccess(reportingContext::finishedPersistingEvents);
                            if(maybePersistedEvents.isLeft()) {
                                CommandHandlingResult<C,E>  rejectionResult = RejectionResult.of(wrapper.commandEnvelope,maybePersistedEvents.getLeft());
                                reportingContext.finishedHandling(rejectionResult);
                                return rejectionResult;
                            } else {
                                CommandHandlingResult<C,E> successResult = SuccessResult.of(wrapper.commandEnvelope, maybePersistedEvents.get().map(PersistedEvent::rawEvent));
                                reportingContext.finishedHandling(successResult);
                                return successResult;
                            }
                        });
                    } else {
                        // TODO: We should capture metrics about duplicated commands
                        // TODO: Capture/log/report on age of duplicate commands
                        // TODO: In theory should query event store specifically for all events matching command id (using snapshots would mean not all events would be read)
                        // TODO: A little inefficient to do a full scan of all events rather than build up a model on first pass (see above)
                        LOGGER.info("Skipped processing duplicate command: " + wrapper.commandEnvelope().command());
                        CommandHandlingResult<C,E> deduplicationSuccessResult = SuccessResult.of(
                                wrapper.commandEnvelope,
                                previousEvents.filter(e -> e.causationId().get().equals(wrapper.commandEnvelope.commandId().get())).map(PersistedEvent::rawEvent), true);
                        reportingContext.finishedHandling(deduplicationSuccessResult);
                        return Future.successful(deduplicationSuccessResult);
                    }
                });
        }

        private Future<Either<Throwable, List<PersistedEvent<A,E>>>> handleAndPersist(
                AggregateRoutingCommandEnvelopeWrapper<C> wrapper,
                AggregateRootRef<A, C, E, State> aggregateRootRef,
                Long expectedSequenceNumber,
                ReportingContext reportingContext) {

            reportingContext.startedApplyingCommand();
            return wrapper.commandEnvelope.correlationId().map( correlationId ->
                aggregateRootRef.handle(wrapper.commandEnvelope.command())
                    .recoverWith( e -> {
                        reportingContext.commandFailed(e);
                        return Future.failed(e);
                    })
                    .flatMap(maybeGeneratedEventsAndState -> {
                        maybeGeneratedEventsAndState.toTry()
                                .onSuccess(eventsAndState -> reportingContext.commandAccepted(eventsAndState._1))
                                .onFailure(reportingContext::commandRejected);
                        return eitherFutureToFutureEither(maybeGeneratedEventsAndState.map(generatedEventsAndState -> {
                            reportingContext.startedPersistingEvents(generatedEventsAndState._1, generatedEventsAndState._2, expectedSequenceNumber);
                            return eventStore.saveEventsAndState(
                                    aggregateType,
                                    wrapper.aggregateId(),
                                    CausationId.of(wrapper.commandEnvelope().commandId().get()),
                                    correlationId,
                                    maybeGeneratedEventsAndState.get()._1,
                                    maybeGeneratedEventsAndState.get()._2,
                                    expectedSequenceNumber).recoverWith( e -> {
                                        reportingContext.finishedPersistingEvents(e);
                                        return Future.failed(e);
                                    });
                        }));
                    })
            ).getOrElse(aggregateRootRef.handle(wrapper.commandEnvelope.command())
                    .recoverWith( e -> {
                        reportingContext.commandFailed(e);
                        return Future.failed(e);
                    })
                    .flatMap(maybeGeneratedEventsAndState -> {
                        maybeGeneratedEventsAndState.toTry()
                                .onSuccess(eventsAndState -> reportingContext.commandAccepted(eventsAndState._1))
                                .onFailure(reportingContext::commandRejected);
                        return eitherFutureToFutureEither(maybeGeneratedEventsAndState.map(generatedEventsAndState -> {
                            reportingContext.startedPersistingEvents(generatedEventsAndState._1, generatedEventsAndState._2, expectedSequenceNumber);
                            return eventStore.saveEventsAndState(
                                    aggregateType,
                                    wrapper.aggregateId(),
                                    CausationId.of(wrapper.commandEnvelope().commandId().get()),
                                    maybeGeneratedEventsAndState.get()._1,
                                    maybeGeneratedEventsAndState.get()._2,
                                    expectedSequenceNumber).recoverWith( e -> {
                                        reportingContext.finishedPersistingEvents(e);
                                        return Future.failed(e);
                                    });
                        }));
                    })
            );
        }

        private Future<Either<Throwable, List<PersistedEvent<A,E>>>> eitherFutureToFutureEither(Either<Throwable,Future<List<PersistedEvent<A,E>>>> either) {
            if(either.isRight()) {
                return either.get().map(Either::right);
            } else {
                return Future.successful(Either.left(either.getLeft()));
            }
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

        public Future<Option<State>> state() {
            Promise<Option<State>> promise = Promise.make();

            if(previousEvents.isEmpty()) {
                promise.success(Option.none());
            } else {
                try {
                    A aggregateInstance = aggregateType.clazz().newInstance();

                    // TODO: Pass snapshot once implemented
                    Behaviour<C, E, State> behaviour = aggregateInstance.initialBehaviour();

                    for (E event : previousEvents) {
                        behaviour = behaviour.handleEvent(event);
                    }

                    promise.success(Option.of(behaviour.state()));
                } catch (Exception ex) {
                    promise.failure(ex);
                }
            }

            return promise.future();
        }

        public Future<Either<Throwable,Tuple2<List<E>,State>>> handle(C command) {
            Promise<Either<Throwable,Tuple2<List<E>,State>>> promise = Promise.make();

            try {
                A aggregateInstance = aggregateType.clazz().newInstance();

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

                handled.bimap(e -> promise.success(Either.left(e)), eventsList -> {
                    // Apply events to get latest state for potential serialisation
                    Behaviour<C, E, State> updatedBehaviour = eventsList.foldLeft(
                            finalBehaviour,
                            Behaviour::handleEvent);

                    return promise.success(Either.right(new Tuple2<>(eventsList, updatedBehaviour.state())));
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

    private class ReportingContext implements CommandHandlingProbe<A,C,E,State> {

        private List<CommandHandlingProbe<A,C,E,State>> probes;

        ReportingContext(AggregateId aggregateId, List<AggregateRepositoryReporter> reporters) {
            probes = reporters.map(reporter -> reporter.createProbe(aggregateType, aggregateId));
        }

        @Override
        public void startedHandling(CommandEnvelope<C> command) {
            probes.forEach(probe -> probe.startedHandling(command));
        }

        @Override
        public void startedLoadingEvents() {
            probes.forEach(CommandHandlingProbe::startedLoadingEvents);
        }

        @Override
        public void finishedLoadingEvents(List<PersistedEvent<A,E>> events) {
            probes.forEach(probe -> probe.finishedLoadingEvents(events));
        }

        @Override
        public void finishedLoadingEvents(Throwable unexpectedException) {
            probes.forEach(probe -> probe.finishedLoadingEvents(unexpectedException));
        }

        @Override
        public void startedApplyingCommand() {
            probes.forEach(CommandHandlingProbe::startedApplyingCommand);
        }

        @Override
        public void commandAccepted(List<? super E> events) {
            probes.forEach(probe -> probe.commandAccepted(events));
        }

        @Override
        public void commandRejected(Throwable rejection) {
            probes.forEach(probe -> probe.commandRejected(rejection));
        }

        @Override
        public void commandFailed(Throwable unexpectedException) {
            probes.forEach(probe -> probe.commandFailed(unexpectedException));
        }

        @Override
        public void startedPersistingEvents(List<? super E> events, long expectedSequenceNumber) {
            probes.forEach(probe -> probe.startedPersistingEvents(events, expectedSequenceNumber));
        }

        @Override
        public void startedPersistingEvents(List<? super E> events, State state, long expectedSequenceNumber) {
            probes.forEach(probe -> probe.startedPersistingEvents(events, state, expectedSequenceNumber));
        }

        @Override
        public void finishedPersistingEvents(List<PersistedEvent<A, E>> persistedEvents) {
            probes.forEach(probe -> probe.finishedPersistingEvents(persistedEvents));
        }

        @Override
        public void finishedPersistingEvents(Throwable unexpectedException) {
            probes.forEach(probe -> probe.finishedPersistingEvents(unexpectedException));
        }

        @Override
        public void finishedHandling(CommandHandlingResult<C, E> result) {
            probes.forEach(probe -> probe.finishedHandling(result));
        }

        @Override
        public void finishedHandling(Throwable unexpectedException) {
            probes.forEach(probe -> probe.finishedHandling(unexpectedException));
        }
    }
}
