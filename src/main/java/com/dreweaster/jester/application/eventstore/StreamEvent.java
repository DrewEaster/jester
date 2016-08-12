package com.dreweaster.jester.application.eventstore;

import com.dreweaster.jester.domain.Aggregate;
import com.dreweaster.jester.domain.DomainEvent;

/**
 */
public interface StreamEvent<A extends Aggregate<?, E, ?>, E extends DomainEvent> extends PersistedEvent<A, E> {

    Long offset();
}
