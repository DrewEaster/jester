package com.dreweaster.jester.commandhandler;

import com.dreweaster.jester.eventstore.PersistedEvent;

/**
 */
public interface CommandDeduplicationStrategyBuilder {

    CommandDeduplicationStrategyBuilder addEvent(PersistedEvent<?, ?> domainEvent);

    CommandDeduplicationStrategy build();
}
