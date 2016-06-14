package com.dreweaster.ddd.framework;

import java.util.List;

public interface EventStore {

    <A, E> List<PersistedEvent<A, E>> loadEvents(Class<A> aggregateType, AggregateId aggregateId);

    <A, E> List<PersistedEvent<A, E>> saveEvents(
            Class<A> aggregateType,
            AggregateId aggregateId,
            CommandId commandId,
            List<E> rawEvents,
            Long expectedSequenceNumber);
}
