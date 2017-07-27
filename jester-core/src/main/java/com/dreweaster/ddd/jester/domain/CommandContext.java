package com.dreweaster.ddd.jester.domain;

import io.vavr.collection.List;
import io.vavr.control.Either;

public interface CommandContext<E extends DomainEvent, State> {

    State currentState();

    AggregateId aggregateId();

    default Either<Throwable, List<E>> success(E... events) {
        return Either.right(List.of(events));
    }

    default Either<Throwable, List<E>> error(Throwable error) {
        return Either.left(error);
    }

    default void invalidCommand(String message) {
        error(new Aggregate.InvalidCommandException(message));
    }
}