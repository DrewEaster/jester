package com.dreweaster.ddd.framework;

import org.reactivestreams.Publisher;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletionStage;

public interface EventStore {

    class OptimisticConcurrencyException extends RuntimeException {

    }

    <A extends Aggregate<?, E, ?>, E extends DomainEvent> CompletionStage<List<PersistedEvent<A, E>>> loadEvents(
            Class<A> aggregateType,
            AggregateId aggregateId);

    <A extends Aggregate<?, E, ?>, E extends DomainEvent> CompletionStage<List<PersistedEvent<A, E>>> saveEvents(
            Class<A> aggregateType,
            AggregateId aggregateId,
            CommandId commandId,
            List<E> rawEvents,
            Long expectedSequenceNumber);

    <A extends Aggregate<?, E, ?>, E extends DomainEvent> Publisher<StreamEvent<A, E>> stream(
            Class<A> aggregateType,
            Optional<Long> fromOffset);
}
