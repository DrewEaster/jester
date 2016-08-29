package com.dreweaster.jester.example.infrastructure.mapper;

import com.dreweaster.jester.example.domain.aggregates.user.events.UserRegistered;
import com.dreweaster.jester.infrastructure.driven.eventstore.serialiser.json.JsonNodeEventMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class UserRegisteredEventMapper extends JsonNodeEventMapper<UserRegistered> {

    @Override
    public JsonNode mapToJson(UserRegistered event, ObjectNode newRoot) {
        newRoot.put("username", event.username());
        newRoot.put("password", event.password());
        return newRoot;
    }

    @Override
    public UserRegistered mapFromJson(JsonNode root) {
        return UserRegistered.builder()
                .username(root.get("username").asText())
                .password(root.get("password").asText())
                .create();
    }
}
