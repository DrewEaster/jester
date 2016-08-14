package com.dreweaster.jester.domain;

import javaslang.concurrent.Future;

import java.util.List;

public interface AggregateRepository<A extends Aggregate<C, E, State>, C extends DomainCommand, E extends DomainEvent, State> {

    public interface AggregateRoot<C extends DomainCommand, E extends DomainEvent> {

        Future<List<? super E>> handle(CommandId commandId, C command);
    }

    AggregateRoot<C, E> aggregateRootOf(AggregateId aggregateId);
}
