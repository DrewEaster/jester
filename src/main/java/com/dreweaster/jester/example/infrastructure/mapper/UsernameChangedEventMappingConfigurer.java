package com.dreweaster.jester.example.infrastructure.mapper;

import com.dreweaster.jester.example.domain.aggregates.user.events.UsernameChanged;
import com.dreweaster.jester.infrastructure.driven.eventstore.mapper.json.JsonEventMappingConfigurer;
import com.dreweaster.jester.infrastructure.driven.eventstore.mapper.json.JsonEventMappingConfigurationFactory;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class UsernameChangedEventMappingConfigurer implements JsonEventMappingConfigurer<UsernameChanged> {

    @Override
    public void configure(JsonEventMappingConfigurationFactory<UsernameChanged> configurationFactory) {
        configurationFactory.create(UsernameChanged.class.getName())
                .mappingFunctions(this::serialise, this::deserialise);
    }

    public JsonNode serialise(UsernameChanged event, ObjectNode newRoot) {
        newRoot.put("username", event.username());
        return newRoot;
    }

    public UsernameChanged deserialise(JsonNode root) {
        return UsernameChanged.builder()
                .username(root.get("username").asText())
                .create();
    }
}
