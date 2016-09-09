package com.dreweaster.ddd.jester.infrastructure.driven.eventstore.mapper.json;

import com.dreweaster.ddd.jester.domain.DomainEvent;

public interface JsonEventMappingConfigurationFactory<T extends DomainEvent> {

    JsonEventMappingConfiguration<T> create(String initialEventClassName);
}
