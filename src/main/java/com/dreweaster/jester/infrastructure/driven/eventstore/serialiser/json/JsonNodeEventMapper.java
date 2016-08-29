package com.dreweaster.jester.infrastructure.driven.eventstore.serialiser.json;

import com.dreweaster.jester.domain.DomainEvent;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

public abstract class JsonNodeEventMapper<T extends DomainEvent> {

    public abstract JsonNode mapToJson(T event, ObjectNode newRoot);

    public abstract T mapFromJson(JsonNode eventJson);
}
