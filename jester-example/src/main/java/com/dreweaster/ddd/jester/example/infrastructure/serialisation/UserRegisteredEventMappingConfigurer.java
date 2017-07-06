package com.dreweaster.ddd.jester.example.infrastructure.serialisation;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.dreweaster.ddd.jester.example.domain.aggregates.user.events.UserRegistered;
import com.dreweaster.ddd.jester.infrastructure.driven.eventstore.mapper.json.JsonEventMappingConfigurer;
import com.dreweaster.ddd.jester.infrastructure.driven.eventstore.mapper.json.JsonEventMappingConfigurationFactory;

public class UserRegisteredEventMappingConfigurer implements JsonEventMappingConfigurer<UserRegistered> {

    @Override
    public void configure(JsonEventMappingConfigurationFactory<UserRegistered> configurationFactory) {
        configurationFactory
                .create(UserRegistered.class.getName())
                .mappingFunctions(this::serialise, this::deserialise);
    }

    private JsonNode serialise(UserRegistered event, ObjectNode root) {
        root.put("username", event.username());
        root.put("password", event.password());
        return root;
    }

    private UserRegistered deserialise(JsonNode root) {
        return UserRegistered.builder()
                .username(root.get("username").asText())
                .password(root.get("password").asText())
                .create();
    }
}
