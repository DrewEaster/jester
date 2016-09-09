package com.dreweaster.ddd.jester.application.repository.deduplicating;


import com.dreweaster.ddd.jester.application.eventstore.PersistedEvent;

/**
 */
public interface CommandDeduplicationStrategyBuilder {

    CommandDeduplicationStrategyBuilder addEvent(PersistedEvent<?, ?> domainEvent);

    CommandDeduplicationStrategy build();
}
