package com.dreweaster.ddd.framework;

import java.time.LocalDate;

/**
 */
public interface PersistedEvent<A extends Aggregate<E, ?>, E extends DomainEvent> {

    Class<A> aggregateType();

    AggregateId aggregateId();

    CommandId commandId();

    E rawEvent();

    LocalDate timestamp();

    Long sequenceNumber();
}
