package com.dreweaster.ddd.framework;

/**
 */
public interface CommandDeduplicationStrategyBuilder {

    CommandDeduplicationStrategyBuilder addDomainEvent(DomainEvent<?, ?> domainEvent);

    CommandDeduplicationStrategy build();
}
