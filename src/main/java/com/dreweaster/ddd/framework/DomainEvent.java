package com.dreweaster.ddd.framework;

import java.time.LocalDate;

/**
 */
public interface DomainEvent<A, E> {

    Class<A> aggregateType();

    AggregateId aggregateId();

    CommandId commandId();

    E rawEvent();

    LocalDate timestamp();

    Long sequenceNumber();
}
