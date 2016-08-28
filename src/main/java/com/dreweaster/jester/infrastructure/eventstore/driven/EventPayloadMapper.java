package com.dreweaster.jester.infrastructure.eventstore.driven;

public interface EventPayloadMapper {

    class MappingException extends RuntimeException {
        public MappingException(String message) {
            super(message);
        }

        public MappingException(Throwable cause) {
            super(cause);
        }
    }

    <E> E deserialise(String payload, Class<E> eventType);

    <E> String serialise(E event);
}
