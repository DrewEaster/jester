package com.dreweaster.jester.example.infrastructure.mapper;

import com.dreweaster.jester.example.domain.aggregates.user.events.UserRegistered;
import com.dreweaster.jester.infrastructure.driven.eventstore.mapper.json.JsonEventMappingConfigurationFactory;
import com.dreweaster.jester.infrastructure.driven.eventstore.mapper.json.JsonEventMappingConfigurer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class UserRegisteredEventMappingConfigurer implements JsonEventMappingConfigurer<UserRegistered> {

    public JsonNode migrateVersion1ToVersion2(JsonNode node) {
        return node;
    }

    public JsonNode migrateVersion2ToVersion3(JsonNode node) {
        return node;
    }

    public JsonNode migrateVersion3ToVersion4(JsonNode node) {
        return node;
    }

    public JsonNode migrateVersion4ToVersion6(JsonNode node) {
        return node;
    }

    public JsonNode migrateVersion6ToVersion8(JsonNode node) {
        return node;
    }

    @Override
    public void configure(JsonEventMappingConfigurationFactory<UserRegistered> configurationFactory) {
        configurationFactory.create("com.dreweaster.domain.UserInstantiated")
                .migrateFormat(this::migrateVersion1ToVersion2)
                .migrateFormat(this::migrateVersion2ToVersion3)
                .migrateFormat(this::migrateVersion3ToVersion4)
                .migrateClassName("com.dreweaster.domain.UserCreated")
                .migrateFormat(this::migrateVersion4ToVersion6)
                .migrateClassName(UserRegistered.class.getName())
                .migrateFormat(this::migrateVersion6ToVersion8)
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
