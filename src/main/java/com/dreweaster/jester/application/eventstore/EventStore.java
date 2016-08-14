package com.dreweaster.jester.application.eventstore;

import com.dreweaster.jester.domain.CommandId;
import com.dreweaster.jester.domain.Aggregate;
import com.dreweaster.jester.domain.AggregateId;
import com.dreweaster.jester.domain.DomainEvent;
import javaslang.concurrent.Future;

import java.util.List;

public interface EventStore {

    class OptimisticConcurrencyException extends RuntimeException {

    }

    <A extends Aggregate<?, E, ?>, E extends DomainEvent> Future<List<PersistedEvent<A, E>>> loadEvents(
            Class<A> aggregateType,
            AggregateId aggregateId);

    <A extends Aggregate<?, E, ?>, E extends DomainEvent> Future<List<PersistedEvent<A, E>>> saveEvents(
            Class<A> aggregateType,
            AggregateId aggregateId,
            CommandId commandId,
            List<E> rawEvents,
            Long expectedSequenceNumber);

/*    <A extends Aggregate<?, E, ?>, E extends DomainEvent> Publisher<StreamEvent<A, E>> stream(
            Class<A> aggregateType,
            Optional<Long> fromOffset);*/
}
