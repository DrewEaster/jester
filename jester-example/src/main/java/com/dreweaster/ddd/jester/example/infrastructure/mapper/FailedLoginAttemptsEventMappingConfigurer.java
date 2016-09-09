package com.dreweaster.ddd.jester.example.infrastructure.mapper;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.dreweaster.ddd.jester.infrastructure.driven.eventstore.mapper.json.JsonEventMappingConfigurer;
import com.dreweaster.ddd.jester.infrastructure.driven.eventstore.mapper.json.JsonEventMappingConfigurationFactory;
import com.dreweaster.ddd.jester.example.domain.aggregates.user.events.*;
import com.dreweaster.ddd.jester.example.domain.aggregates.user.commands.*;
import com.dreweaster.ddd.jester.example.domain.aggregates.user.UserState;

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
