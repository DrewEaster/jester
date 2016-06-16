package com.dreweaster.ddd.framework;

import java.util.List;
import java.util.concurrent.CompletionStage;

public interface EventStore {

    <A extends Aggregate<?, E, ?>, E extends DomainEvent> CompletionStage<List<PersistedEvent<A, E>>> loadEvents(
            Class<A> aggregateType,
            AggregateId aggregateId);

    <A extends Aggregate<?, E, ?>, E extends DomainEvent> CompletionStage<List<PersistedEvent<A, E>>> saveEvents(
            Class<A> aggregateType,
            AggregateId aggregateId,
            CommandId commandId,
            List<E> rawEvents,
            Long expectedSequenceNumber);
}
