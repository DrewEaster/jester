package com.dreweaster.ddd.jester.application.eventstore;

import com.dreweaster.ddd.jester.domain.DomainEvent;
import javaslang.Tuple2;

public interface EventPayloadMapper {

    class MappingException extends RuntimeException {

        public MappingException(String message) {
            super(message);
        }

        public MappingException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    <T extends DomainEvent> T deserialise(
            String serialisedPayload,
            String serialisedEventType,
            Integer serialisedEventVersion);

    <T extends DomainEvent> Tuple2<String, Integer> serialise(T event);
}
