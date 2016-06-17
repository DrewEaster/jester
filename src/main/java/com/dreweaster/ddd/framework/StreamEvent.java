package com.dreweaster.ddd.framework;

/**
 */
public interface StreamEvent<A extends Aggregate<?, E, ?>, E extends DomainEvent> extends PersistedEvent<A, E> {

    Long offset();
}
