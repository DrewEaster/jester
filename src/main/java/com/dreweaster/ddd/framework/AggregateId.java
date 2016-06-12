package com.dreweaster.ddd.framework;

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
}
