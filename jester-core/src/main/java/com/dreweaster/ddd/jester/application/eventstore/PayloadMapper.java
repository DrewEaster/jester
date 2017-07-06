package com.dreweaster.ddd.jester.application.eventstore;

import com.dreweaster.ddd.jester.domain.Aggregate;
import com.dreweaster.ddd.jester.domain.DomainEvent;
import javaslang.control.Option;

public interface PayloadMapper {

    class PayloadSerialisationResult {

        public static PayloadSerialisationResult of(String payload, SerialisationContentType contentType, Integer version) {
            return new PayloadSerialisationResult(payload, contentType, Option.of(version));
        }

        public static PayloadSerialisationResult of(String payload, SerialisationContentType contentType) {
            return new PayloadSerialisationResult(payload, contentType, Option.none());
        }

        private String payload;

        private Option<Integer> version;

        private SerialisationContentType contentType;

        private PayloadSerialisationResult(String payload, SerialisationContentType contentType, Option<Integer> version) {
            this.payload = payload;
            this.version = version;
            this.contentType = contentType;
        }

        public String payload() {
            return payload;
        }

        public Option<Integer> version() {
            return version;
        }

        public SerialisationContentType contentType() {
            return contentType;
        }
    }

    class MappingException extends RuntimeException {

        public MappingException(String message) {
            super(message);
        }

        public MappingException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    <T extends DomainEvent> T deserialiseEvent(
            String serialisedPayload,
            String serialisedEventType,
            Integer serialisedEventVersion);

    <T extends DomainEvent> PayloadSerialisationResult serialiseEvent(T event);

    <A extends Aggregate<?, ?, State>, State> PayloadSerialisationResult serialiseState(State state);
}
