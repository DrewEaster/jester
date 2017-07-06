package com.dreweaster.ddd.jester.application.eventstore;

/**
 */
public enum SerialisationContentType {

    JSON("application/json");

    private String value;

    SerialisationContentType(String value) {
        this.value = value;
    }

    String value() {
        return value;
    }
}
