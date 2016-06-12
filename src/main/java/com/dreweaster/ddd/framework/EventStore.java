package com.dreweaster.ddd.framework;

import java.util.List;

public interface EventStore {

    <A, E> List<DomainEvent<A, E>> loadEvents(Class<A> aggregateType, AggregateId aggregateId);

    <A, E> List<DomainEvent<A, E>> saveEvents(
            Class<A> aggregateType,
            AggregateId aggregateId,
            CommandId commandId,
            List<E> rawEvents,
            Long expectedSequenceNumber);
}
