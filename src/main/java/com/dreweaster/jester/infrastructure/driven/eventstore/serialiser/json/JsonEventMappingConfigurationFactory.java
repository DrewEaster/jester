package com.dreweaster.jester.infrastructure.driven.eventstore.serialiser.json;

import com.dreweaster.jester.domain.DomainEvent;

public interface JsonEventMappingConfigurationFactory<T extends DomainEvent> {

    JsonEventMappingConfiguration<T> create(String initialEventClassName);
}
