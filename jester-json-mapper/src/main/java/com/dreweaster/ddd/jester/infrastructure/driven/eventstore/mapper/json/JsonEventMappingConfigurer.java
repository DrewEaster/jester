package com.dreweaster.ddd.jester.infrastructure.driven.eventstore.mapper.json;


import com.dreweaster.ddd.jester.domain.DomainEvent;

public interface JsonEventMappingConfigurer<T extends DomainEvent> {

    void configure(JsonEventMappingConfigurationFactory<T> configurationFactory);
}
