package com.dreweaster.ddd.jester.application.eventstore;

import com.dreweaster.ddd.jester.domain.Aggregate;
import com.dreweaster.ddd.jester.domain.DomainEvent;

/**
 */
public interface StreamEvent<A extends Aggregate<?, E, ?>, E extends DomainEvent> extends PersistedEvent<A, E> {

    Long offset();
}
