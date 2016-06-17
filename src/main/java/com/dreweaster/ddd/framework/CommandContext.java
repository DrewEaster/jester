package com.dreweaster.ddd.framework;

import java.util.List;

public interface CommandContext<E extends DomainEvent, State> {

    State currentState();

    AggregateId aggregateId();

    void success(List<E> events);

    void success(E event);

    void error(Throwable error);

    default void invalidCommand(String message) {
        error(new Aggregate.InvalidCommandException(message));
    }
}