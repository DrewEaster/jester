package com.dreweaster.jester.application.repository.deduplicating;

import com.dreweaster.jester.application.eventstore.PersistedEvent;

/**
 */
public interface CommandDeduplicationStrategyBuilder {

    CommandDeduplicationStrategyBuilder addEvent(PersistedEvent<?, ?> domainEvent);

    CommandDeduplicationStrategy build();
}
