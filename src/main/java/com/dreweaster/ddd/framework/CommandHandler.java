package com.dreweaster.ddd.framework;

import rx.Single;

import java.util.List;

public interface CommandHandler<A extends Aggregate<C, E, ?>, C extends DomainCommand, E extends DomainEvent> {

    Single<List<PersistedEvent<A, E>>> handle(CommandEnvelope<? extends C> command);
}
