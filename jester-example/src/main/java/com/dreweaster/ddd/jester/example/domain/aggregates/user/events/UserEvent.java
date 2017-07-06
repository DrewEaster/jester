package com.dreweaster.ddd.jester.example.domain.aggregates.user.events;

import com.dreweaster.ddd.jester.domain.DomainEvent;
import com.dreweaster.ddd.jester.domain.DomainEventTag;

/**
 */
public interface UserEvent extends DomainEvent {

    DomainEventTag TAG = DomainEventTag.of("user-event");

    @Override
    default DomainEventTag tag() {
        return TAG;
    }
}
