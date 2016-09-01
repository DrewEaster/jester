package com.dreweaster.jester.example.infrastructure.mapper;

import com.dreweaster.jester.example.domain.aggregates.user.events.UserLocked;
import com.dreweaster.jester.infrastructure.driven.eventstore.mapper.json.JsonEventMappingConfigurer;
import com.dreweaster.jester.infrastructure.driven.eventstore.mapper.json.JsonEventMappingConfigurationFactory;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class UserLockedEventMappingConfigurer implements JsonEventMappingConfigurer<UserLocked> {

    @Override
    public void configure(JsonEventMappingConfigurationFactory<UserLocked> configurationFactory) {
        configurationFactory.create(UserLocked.class.getName())
                .objectMappers(this::serialise, this::deserialise);
    }

    public JsonNode serialise(UserLocked event, ObjectNode newRoot) {
        return newRoot;
    }

    public UserLocked deserialise(JsonNode root) {
        return UserLocked.of();
    }
}
