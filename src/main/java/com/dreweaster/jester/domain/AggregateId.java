package com.dreweaster.jester.domain;

/**
 */
public class AggregateId {

    public static AggregateId of(String id) {
        return new AggregateId(id);
    }

    private String id;

    private AggregateId(String id) {
        this.id = id;
    }

    public String get() {
        return id;
    }

    @Override
    public String toString() {
        return "AggregateId{" +
                "id='" + id + '\'' +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        AggregateId that = (AggregateId) o;

        return id.equals(that.id);

    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }
}
