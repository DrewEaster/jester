package com.dreweaster.ddd.framework;

import java.util.List;

public interface AggregateRootFactory {

    <A, C, E> AggregateRootRef<C, E> aggregateOf(Class<A> aggregateType, AggregateId aggregateId, List<E> previousEvents);
}
