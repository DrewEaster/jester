package com.dreweaster.ddd.jester.example.infrastructure.serialisation;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.dreweaster.ddd.jester.infrastructure.driven.eventstore.mapper.json.JsonEventMappingConfigurer;
import com.dreweaster.ddd.jester.infrastructure.driven.eventstore.mapper.json.JsonEventMappingConfigurationFactory;
import com.dreweaster.ddd.jester.example.domain.aggregates.user.events.*;

public class UserLockedEventMappingConfigurer implements JsonEventMappingConfigurer<UserLocked> {

    @Override
    public void configure(JsonEventMappingConfigurationFactory<UserLocked> configurationFactory) {
        configurationFactory.create(UserLocked.class.getName())
                .mappingFunctions(this::serialise, this::deserialise);
    }

    public JsonNode serialise(UserLocked event, ObjectNode newRoot) {
        return newRoot;
    }

    public UserLocked deserialise(JsonNode root) {
        return UserLocked.of();
    }
}
