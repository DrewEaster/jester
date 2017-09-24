package com.dreweaster.ddd.jester.domain;

import java.util.UUID;

/**
 */
public class EventId {

    public static EventId of(String eventId) {
        return new EventId(eventId);
    }

    public static EventId createUnique() {
        return new EventId(UUID.randomUUID().toString().replaceAll("-",""));
    }

    private String id;

    private EventId(String id) {
        this.id = id;
    }

    public String get() {
        return id;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        EventId eventId = (EventId) o;

        if (!id.equals(eventId.id)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public String toString() {
        return "EventId{" +
                "id='" + id + '\'' +
                '}';
    }
}
