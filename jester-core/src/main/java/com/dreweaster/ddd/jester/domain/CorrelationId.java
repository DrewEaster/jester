package com.dreweaster.ddd.jester.domain;

import io.vavr.control.Option;

/**
 */
public class CorrelationId {

    public static Option<CorrelationId> ofNullable(String id) {
        return id == null ? Option.none() : Option.of(CorrelationId.of(id));
    }

    public static CorrelationId of(String id) {
        if (id == null) {
            throw new IllegalArgumentException("id cannot be null");
        }
        return new CorrelationId(id);
    }

    private String id;

    private CorrelationId(String id) {
        this.id = id;
    }

    public String get() {
        return id;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        CorrelationId correlationId = (CorrelationId) o;

        if (!id.equals(correlationId.id)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public String toString() {
        return "CorrelationId{" +
                "id='" + id + '\'' +
                '}';
    }
}
