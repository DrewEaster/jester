package com.dreweaster.ddd.jester.application.eventstore;

import com.dreweaster.ddd.jester.domain.*;
import io.vavr.control.Option;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZonedDateTime;

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

    Instant timestamp();

    Long sequenceNumber();
}
