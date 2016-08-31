package com.dreweaster.jester.application.eventstore;

import com.dreweaster.jester.domain.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * TODO: Add Option<CorrelationId>
 * TODO: Add EventId
 * TODO: Add Option<CausationId>
 */
public interface PersistedEvent<A extends Aggregate<?, E, ?>, E extends DomainEvent> {

    AggregateType<A, ?, E, ?> aggregateType();

    AggregateId aggregateId();

    CommandId commandId();

    Class<E> eventType();

    Integer eventVersion();

    E rawEvent();

    LocalDateTime timestamp();

    Long sequenceNumber();
}
