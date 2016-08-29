package com.dreweaster.jester.example.infrastructure.mapper;

import com.dreweaster.jester.example.domain.aggregates.user.events.UserLocked;
import com.dreweaster.jester.infrastructure.driven.eventstore.serialiser.json.JsonNodeEventMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class UserLockedEventMapper extends JsonNodeEventMapper<UserLocked> {

    @Override
    public JsonNode mapToJson(UserLocked event, ObjectNode newRoot) {
        return newRoot;
    }

    @Override
    public UserLocked mapFromJson(JsonNode root) {
        return UserLocked.of();
    }
}
