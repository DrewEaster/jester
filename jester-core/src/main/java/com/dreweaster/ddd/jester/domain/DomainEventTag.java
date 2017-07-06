package com.dreweaster.ddd.jester.domain;

public class DomainEventTag {

    public static DomainEventTag of(String id) {
        return new DomainEventTag(id);
    }

    private String tag;

    private DomainEventTag(String tag) {
        this.tag = tag;
    }

    public String tag() {
        return tag;
    }
}
