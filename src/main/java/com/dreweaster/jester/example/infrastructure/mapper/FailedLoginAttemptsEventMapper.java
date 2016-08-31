package com.dreweaster.jester.example.infrastructure.mapper;

import com.dreweaster.jester.example.domain.aggregates.user.events.FailedLoginAttemptsIncremented;
import com.dreweaster.jester.infrastructure.driven.eventstore.serialiser.json.JsonEventMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

public class FailedLoginAttemptsEventMapper extends JsonEventMapper<FailedLoginAttemptsIncremented> {

    @Override
    public JsonNode mapToJson(FailedLoginAttemptsIncremented event, ObjectNode newRoot) {
        return newRoot;
    }

    @Override
    public FailedLoginAttemptsIncremented mapFromJson(JsonNode root) {
        return FailedLoginAttemptsIncremented.of();
    }
}
