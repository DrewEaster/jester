package com.dreweaster.jester.application.eventstore;

import com.dreweaster.jester.domain.CommandId;
import com.dreweaster.jester.domain.Aggregate;
import com.dreweaster.jester.domain.AggregateId;
import com.dreweaster.jester.domain.DomainEvent;

import java.time.LocalDate;

/**
 */
public interface PersistedEvent<A extends Aggregate<?, E, ?>, E extends DomainEvent> {

    Class<A> aggregateType();

    AggregateId aggregateId();

    CommandId commandId();

    Class<E> eventType();

    E rawEvent();

    LocalDate timestamp();

    Long sequenceNumber();
}
