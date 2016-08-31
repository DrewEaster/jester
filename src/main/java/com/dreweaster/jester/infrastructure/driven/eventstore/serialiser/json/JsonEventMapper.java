package com.dreweaster.jester.infrastructure.driven.eventstore.serialiser.json;

import com.dreweaster.jester.domain.DomainEvent;

public interface JsonEventMapper<T extends DomainEvent> {

    void configure(JsonEventMappingConfigurationFactory<T> configurationFactory);
}
