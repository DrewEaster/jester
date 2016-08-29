package com.dreweaster.jester.application.eventstore;

import com.dreweaster.jester.domain.DomainEvent;

public interface EventPayloadSerialiser {

    class MappingException extends RuntimeException {
        public MappingException(String message) {
            super(message);
        }

        public MappingException(Throwable cause) {
            super(cause);
        }
    }

    <T extends DomainEvent> T deserialise(String payload, Class<T> eventType);

    <T extends DomainEvent> String serialise(T event);
}
