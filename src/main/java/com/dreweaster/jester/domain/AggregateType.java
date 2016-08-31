package com.dreweaster.jester.domain;

public class AggregateType<A extends Aggregate<C, E, State>, C extends DomainCommand, E extends DomainEvent, State> {

    private String name;

    private Class<A> clazz;

    public static <A extends Aggregate<C, E, State>, C extends DomainCommand, E extends DomainEvent, State> AggregateType<A, C, E, State> of(
            String name, Class<A> clazz) {
        return new AggregateType<>(name, clazz);
    }

    private AggregateType(String name, Class<A> clazz) {
        this.name = name;
        this.clazz = clazz;
    }

    public String name() {
        return name;
    }

    public Class<A> clazz() {
        return clazz;
    }
}
