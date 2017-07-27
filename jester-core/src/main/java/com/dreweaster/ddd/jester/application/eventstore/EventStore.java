package com.dreweaster.ddd.jester.application.eventstore;

import com.dreweaster.ddd.jester.domain.*;
import io.vavr.collection.List;
import io.vavr.concurrent.Future;

public interface EventStore {

    class OptimisticConcurrencyException extends RuntimeException {

    }

    <A extends Aggregate<?, E, State>, E extends DomainEvent, State> Future<List<PersistedEvent<A, E>>> loadEvents(
            AggregateType<A, ?, E, State> aggregateType,
            AggregateId aggregateId);

    <A extends Aggregate<?, E, State>, E extends DomainEvent, State> Future<List<PersistedEvent<A, E>>> loadEvents(
            AggregateType<A, ?, E, State> aggregateType,
            AggregateId aggregateId,
            Long afterSequenceNumber);

    <A extends Aggregate<?, E, State>, E extends DomainEvent, State> Future<List<StreamEvent<A, E>>> loadEventStream(
            AggregateType<A, ?, E, State> aggregateType,
            Integer batchSize);

    <A extends Aggregate<?, E, State>, E extends DomainEvent, State> Future<List<StreamEvent<A, E>>> loadEventStream(
            AggregateType<A, ?, E, State> aggregateType,
            Long afterOffset,
            Integer batchSize);

    <E extends DomainEvent> Future<List<StreamEvent<?, E>>> loadEventStream(
            DomainEventTag tag,
            Long afterOffset,
            Integer batchSize);

    /**
     * @param aggregateType
     * @param aggregateId
     * @param causationId
     * @param rawEvents
     * @param expectedSequenceNumber the last known event sequence number for the corresponding aggregate. -1 to indicate
     *                               you expect this aggregate to have no previous events
     * @param <A>                    the aggregate type
     * @param <E>                    the type of events this aggregate emits
     * @return list of events that have been persisted
     */
    <A extends Aggregate<?, E, State>, E extends DomainEvent, State> Future<List<PersistedEvent<A, E>>> saveEvents(
            AggregateType<A, ?, E, State> aggregateType,
            AggregateId aggregateId,
            CausationId causationId,
            List<E> rawEvents,
            Long expectedSequenceNumber);

    <A extends Aggregate<?, E, State>, E extends DomainEvent, State> Future<List<PersistedEvent<A, E>>> saveEventsAndState(
            AggregateType<A, ?, E, State> aggregateType,
            AggregateId aggregateId,
            CausationId causationId,
            List<E> rawEvents,
            State state,
            Long expectedSequenceNumber);

    /**
     * @param aggregateType
     * @param aggregateId
     * @param causationId
     * @param correlationId
     * @param rawEvents
     * @param expectedSequenceNumber the last known event sequence number for the corresponding aggregate. -1 to indicate
     *                               you expect this aggregate to have no previous events
     * @param <A>                    the aggregate type
     * @param <E>                    the type of events this aggregate emits
     * @return list of events that have been persisted
     */
    <A extends Aggregate<?, E, State>, E extends DomainEvent, State> Future<List<PersistedEvent<A, E>>> saveEvents(
            AggregateType<A, ?, E, State> aggregateType,
            AggregateId aggregateId,
            CausationId causationId,
            CorrelationId correlationId,
            List<E> rawEvents,
            Long expectedSequenceNumber);

    <A extends Aggregate<?, E, State>, E extends DomainEvent, State> Future<List<PersistedEvent<A, E>>> saveEventsAndState(
            AggregateType<A, ?, E, State> aggregateType,
            AggregateId aggregateId,
            CausationId causationId,
            CorrelationId correlationId,
            List<E> rawEvents,
            State state,
            Long expectedSequenceNumber);
}
