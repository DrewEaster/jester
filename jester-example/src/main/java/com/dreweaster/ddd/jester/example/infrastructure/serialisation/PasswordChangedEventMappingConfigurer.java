package com.dreweaster.ddd.jester.example.infrastructure.serialisation;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.dreweaster.ddd.jester.infrastructure.driven.eventstore.mapper.json.JsonEventMappingConfigurer;
import com.dreweaster.ddd.jester.infrastructure.driven.eventstore.mapper.json.JsonEventMappingConfigurationFactory;
import com.dreweaster.ddd.jester.example.domain.aggregates.user.events.*;

public class PasswordChangedEventMappingConfigurer implements JsonEventMappingConfigurer<PasswordChanged> {

    @Override
    public void configure(JsonEventMappingConfigurationFactory<PasswordChanged> configurationFactory) {
        configurationFactory.create(PasswordChanged.class.getName())
                .mappingFunctions(this::serialise, this::deserialise);
    }

    public JsonNode serialise(PasswordChanged event, ObjectNode newRoot) {
        newRoot.put("old_password", event.oldPassword());
        newRoot.put("password", event.password());
        return newRoot;
    }

    public PasswordChanged deserialise(JsonNode root) {
        return PasswordChanged.builder()
                .oldPassword(root.get("old_password").asText())
                .password(root.get("password").asText())
                .create();
    }
}
