package com.dreweaster.ddd.jester.application.eventstore;

import io.vavr.control.Option;

import java.time.LocalDateTime;

public interface StreamEvent {

    Long offset();

    String id();

    String aggregateType();

    String aggregateId();

    String causationId();

    Option<String> correlationId();

    String eventType();

    String eventTag();

    LocalDateTime timestamp();

    Long sequenceNumber();

    String serialisedPayload();

    SerialisationContentType payloadContentType();
}
