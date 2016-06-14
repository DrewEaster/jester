package com.dreweaster.ddd.framework;

import java.util.List;

public interface AggregateRootFactory {

    <A extends Aggregate<E, ?>, E extends DomainEvent> AggregateRootRef<E> aggregateOf(
            Class<A> aggregateType,
            AggregateId aggregateId,
            List<E> previousEvents);
}
