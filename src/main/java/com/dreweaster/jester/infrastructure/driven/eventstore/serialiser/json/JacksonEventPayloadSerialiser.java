package com.dreweaster.jester.infrastructure.driven.eventstore.serialiser.json;

import com.dreweaster.jester.application.eventstore.EventPayloadSerialiser;
import com.dreweaster.jester.domain.DomainEvent;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import javaslang.collection.HashMap;
import javaslang.collection.Map;
import javaslang.control.Option;

import java.io.IOException;

public class JacksonEventPayloadSerialiser implements EventPayloadSerialiser {

    private Map<Class<? extends DomainEvent>, JsonNodeEventMapper<? extends DomainEvent>> mappers = HashMap.empty();

    private ObjectMapper objectMapper;

    public JacksonEventPayloadSerialiser(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public final <T extends DomainEvent> void register(Class<T> domainEventType, JsonNodeEventMapper<T> mapper) {
        mappers = mappers.put(domainEventType, mapper);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T extends DomainEvent> T deserialise(String payload, Class<T> eventType) {
        Option<JsonNodeEventMapper<?>> mapperOpt = mappers.get(eventType);
        JsonNodeEventMapper<T> mapper = (JsonNodeEventMapper<T>) mapperOpt.getOrElseThrow(() ->
                new MappingException("No JSON mapper registered for event type: " + eventType.getName()));

        try {
            JsonNode root = objectMapper.readTree(payload);
            return mapper.mapFromJson(root);
        } catch (IOException ex) {
            throw new MappingException(ex);
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T extends DomainEvent> String serialise(T event) {
        Option<JsonNodeEventMapper<?>> mapperOpt = mappers.get(event.getClass());
        JsonNodeEventMapper<T> mapper = (JsonNodeEventMapper<T>) mapperOpt.getOrElseThrow(() ->
                new MappingException("No JSON mapper registered for event type: " + event.getClass().getName()));
        JsonNode root = mapper.mapToJson(event, objectMapper.createObjectNode());
        return root.toString();
    }
}
