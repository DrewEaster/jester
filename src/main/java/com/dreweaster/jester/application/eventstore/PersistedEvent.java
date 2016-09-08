package com.dreweaster.jester.application.eventstore;

import com.dreweaster.jester.domain.*;
import javaslang.control.Option;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 */
public interface PersistedEvent<A extends Aggregate<?, E, ?>, E extends DomainEvent> {

    EventId id();

    AggregateType<A, ?, E, ?> aggregateType();

    AggregateId aggregateId();

    CausationId causationId();

    Option<CorrelationId> correlationId();

    Class<E> eventType();

    Integer eventVersion();

    E rawEvent();

    LocalDateTime timestamp();

    Long sequenceNumber();
}
