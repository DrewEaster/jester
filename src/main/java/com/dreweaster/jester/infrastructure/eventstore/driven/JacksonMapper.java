package com.dreweaster.jester.infrastructure.eventstore.driven;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;

import java.io.IOException;

/**
 */
public class JacksonMapper implements EventPayloadMapper {

    public static JacksonMapper newSnakeCaseObjectMapper() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.setPropertyNamingStrategy(PropertyNamingStrategy.SNAKE_CASE);
        return new JacksonMapper(objectMapper);
    }

    private ObjectMapper objectMapper;

    private JacksonMapper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public <E> E deserialise(String payload, Class<E> eventType) {
        try {
            return objectMapper.readValue(payload, eventType);
        } catch (IOException e) {
            throw new MappingException(e);
        }
    }

    @Override
    public <E> String serialise(E event) {
        try {
            return objectMapper.writeValueAsString(event);
        } catch (JsonProcessingException e) {
            throw new MappingException(e);
        }
    }
}
