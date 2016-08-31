package com.dreweaster.jester.example.infrastructure.mapper;

import com.dreweaster.jester.example.domain.aggregates.user.events.UsernameChanged;
import com.dreweaster.jester.infrastructure.driven.eventstore.serialiser.json.JsonEventMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class UsernameChangedEventMapper extends JsonEventMapper<UsernameChanged> {

    @Override
    public JsonNode mapToJson(UsernameChanged event, ObjectNode newRoot) {
        newRoot.put("username", event.username());
        return newRoot;
    }

    @Override
    public UsernameChanged mapFromJson(JsonNode root) {
        return UsernameChanged.builder()
                .username(root.get("username").asText())
                .create();
    }
}
