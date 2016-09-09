package com.dreweaster.ddd.jester.application.eventstore;

import com.dreweaster.ddd.jester.domain.*;
import javaslang.control.Option;

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
