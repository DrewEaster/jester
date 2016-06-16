package com.dreweaster.ddd.framework;

import java.util.List;

public interface AggregateRootFactory {

    <A extends Aggregate<C, E, State>, C extends DomainCommand, E extends DomainEvent, State> AggregateRootRef<C, E> aggregateOf(
            Class<A> aggregateType,
            AggregateId aggregateId,
            List<E> previousEvents);
}
