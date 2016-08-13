package com.dreweaster.jester.domain;

import java.util.List;
import java.util.concurrent.CompletionStage;

public interface AggregateRepository<A extends Aggregate<C, E, State>, C extends DomainCommand, E extends DomainEvent, State> {

    public interface AggregateRoot<C extends DomainCommand, E extends DomainEvent> {

        CompletionStage<List<? super E>> handle(CommandId commandId, C command);
    }

    AggregateRoot<C, E> aggregateRootOf(AggregateId aggregateId);
}
