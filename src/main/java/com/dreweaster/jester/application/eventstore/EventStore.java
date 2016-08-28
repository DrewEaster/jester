package com.dreweaster.jester.application.eventstore;

import com.dreweaster.jester.domain.CommandId;
import com.dreweaster.jester.domain.Aggregate;
import com.dreweaster.jester.domain.AggregateId;
import com.dreweaster.jester.domain.DomainEvent;
import javaslang.collection.List;
import javaslang.concurrent.Future;

public interface EventStore {

    class OptimisticConcurrencyException extends RuntimeException {

    }

    <A extends Aggregate<?, E, ?>, E extends DomainEvent> Future<List<PersistedEvent<A, E>>> loadEvents(
            Class<A> aggregateType,
            AggregateId aggregateId);

    <A extends Aggregate<?, E, ?>, E extends DomainEvent> Future<List<PersistedEvent<A, E>>> loadEvents(
            Class<A> aggregateType,
            AggregateId aggregateId,
            Long afterSequenceNumber);

    <A extends Aggregate<?, E, ?>, E extends DomainEvent> Future<List<StreamEvent<A, E>>> loadEventStream(
            Class<A> aggregateType,
            Integer batchSize);

    <A extends Aggregate<?, E, ?>, E extends DomainEvent> Future<List<StreamEvent<A, E>>> loadEventStream(
            Class<A> aggregateType,
            Long afterOffset,
            Integer batchSize);

    /**
     *
     * @param aggregateType
     * @param aggregateId
     * @param commandId
     * @param rawEvents
     * @param expectedSequenceNumber the last known event sequence number for the corresponding aggregate. -1 to indicate
     *                               you expect this aggregate to have no previous events
     * @param <A> the aggregate type
     * @param <E> the type of events this aggregate emits
     * @return list of events that have been persisted
     */
    <A extends Aggregate<?, E, ?>, E extends DomainEvent> Future<List<PersistedEvent<A, E>>> saveEvents(
            Class<A> aggregateType,
            AggregateId aggregateId,
            CommandId commandId,
            List<E> rawEvents,
            Long expectedSequenceNumber);
}
