package com.dreweaster.ddd.framework;

/**
 */
public interface CommandDeduplicationStrategyBuilder {

    CommandDeduplicationStrategyBuilder addEvent(PersistedEvent<?, ?> domainEvent);

    CommandDeduplicationStrategy build();
}
