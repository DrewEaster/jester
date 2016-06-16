package com.dreweaster.ddd.framework;

import rx.Single;

import java.util.List;
import java.util.concurrent.CompletionStage;

public interface CommandHandler<A extends Aggregate<C, E, State>, C extends DomainCommand, E extends DomainEvent, State> {

    CompletionStage<List<PersistedEvent<A, E>>> handle(CommandEnvelope<? extends C> command);
}
