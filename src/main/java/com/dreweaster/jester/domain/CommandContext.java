package com.dreweaster.jester.domain;

import javaslang.control.Either;

import java.util.Arrays;
import java.util.List;

public interface CommandContext<E extends DomainEvent, State> {

    State currentState();

    AggregateId aggregateId();

    default Either<Throwable, List<E>> success(E... events) {
        return Either.right(Arrays.asList(events));
    }

    default Either<Throwable, List<E>> error(Throwable error) {
        return Either.left(error);
    }

    default void invalidCommand(String message) {
        error(new Aggregate.InvalidCommandException(message));
    }
}