package com.dreweaster.jester.domain;

import java.util.List;
import java.util.concurrent.CompletionStage;
import java.util.function.BiFunction;

public interface AggregateRepository<A extends Aggregate<C, E, State>, C extends DomainCommand, E extends DomainEvent, State> {

    BiFunction<CommandId, ? super C, CompletionStage<List<? super E>>> aggregateRootOf(AggregateId aggregateId);
}
