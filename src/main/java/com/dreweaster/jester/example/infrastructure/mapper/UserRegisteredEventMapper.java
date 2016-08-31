package com.dreweaster.jester.example.infrastructure.mapper;

import com.dreweaster.jester.example.domain.aggregates.user.events.UserRegistered;
import com.dreweaster.jester.infrastructure.driven.eventstore.serialiser.json.JsonEventMappingConfigurationFactory;
import com.dreweaster.jester.infrastructure.driven.eventstore.serialiser.json.JsonEventMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class UserRegisteredEventMapper implements JsonEventMapper<UserRegistered> {

    /*@Override
    public void configure(JsonEventMappingConfigurationFactory<UserRegistered> configurationFactory) {
        configurationFactory.create("com.dreweaster.domain.UserCreated")
                .migrateFormat(this::mapVersion1ToVersion2)
                .migrateFormat(this::mapVersion2ToVersion3)
                .migrateFormat(this::mapVersion3ToVersion4)
                .migrateClassName(UserRegistered.class.getName())
                .mapper(serialise(), deserialise());
    }*/

    @Override
    public void configure(JsonEventMappingConfigurationFactory<UserRegistered> configurationFactory) {
        configurationFactory
                .create(UserRegistered.class.getName())
                .mapper(this::serialise, this::deserialise);
    }

    private JsonNode serialise(UserRegistered event, ObjectNode newRoot) {
        newRoot.put("username", event.username());
        newRoot.put("password", event.password());
        return newRoot;
    }

    private UserRegistered deserialise(JsonNode root) {
        return UserRegistered.builder()
                .username(root.get("username").asText())
                .password(root.get("password").asText())
                .create();
    }
}
