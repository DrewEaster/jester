package com.dreweaster.jester.example.infrastructure.mapper;

import com.dreweaster.jester.example.domain.aggregates.user.events.PasswordChanged;
import com.dreweaster.jester.infrastructure.driven.eventstore.serialiser.json.JsonEventMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class PasswordChangedEventMapper extends JsonEventMapper<PasswordChanged> {

    @Override
    public JsonNode mapToJson(PasswordChanged event, ObjectNode newRoot) {
        newRoot.put("old_password", event.oldPassword());
        newRoot.put("password", event.password());
        return newRoot;
    }

    @Override
    public PasswordChanged mapFromJson(JsonNode root) {
        return PasswordChanged.builder()
                .oldPassword(root.get("old_password").asText())
                .password(root.get("password").asText())
                .create();
    }
}
