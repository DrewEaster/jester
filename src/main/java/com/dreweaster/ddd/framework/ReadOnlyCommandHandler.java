package com.dreweaster.ddd.framework;

public interface ReadOnlyCommandHandler<A extends Aggregate<E, ?>, E extends DomainEvent> {

    <R, C extends ReadOnlyDomainCommand<R>> R handle(CommandEnvelope<C> command);
}
