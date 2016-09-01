package com.dreweaster.jester.infrastructure.driven.eventstore.mapper.json;

import com.dreweaster.jester.domain.DomainEvent;

public interface JsonEventMappingConfigurer<T extends DomainEvent> {

    void configure(JsonEventMappingConfigurationFactory<T> configurationFactory);
}
