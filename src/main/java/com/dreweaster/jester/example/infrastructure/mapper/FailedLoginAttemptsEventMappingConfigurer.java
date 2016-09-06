package com.dreweaster.jester.example.infrastructure.mapper;

import com.dreweaster.jester.example.domain.aggregates.user.events.FailedLoginAttemptsIncremented;
import com.dreweaster.jester.infrastructure.driven.eventstore.mapper.json.JsonEventMappingConfigurer;
import com.dreweaster.jester.infrastructure.driven.eventstore.mapper.json.JsonEventMappingConfigurationFactory;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class FailedLoginAttemptsEventMappingConfigurer implements JsonEventMappingConfigurer<FailedLoginAttemptsIncremented> {

    @Override
    public void configure(JsonEventMappingConfigurationFactory<FailedLoginAttemptsIncremented> configurationFactory) {
        configurationFactory.create(FailedLoginAttemptsIncremented.class.getName())
                .mappingFunctions(this::serialise, this::deserialise);
    }

    public JsonNode serialise(FailedLoginAttemptsIncremented event, ObjectNode newRoot) {
        return newRoot;
    }

    public FailedLoginAttemptsIncremented deserialise(JsonNode root) {
        return FailedLoginAttemptsIncremented.of();
    }
}
