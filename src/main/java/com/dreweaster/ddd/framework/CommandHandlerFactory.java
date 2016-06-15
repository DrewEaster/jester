package com.dreweaster.ddd.framework;

/**
 */
public interface CommandHandlerFactory {

    <A extends Aggregate<C, E, ?>, C extends DomainCommand, E extends DomainEvent> CommandHandler<A, C, E> handlerFor(Class<A> aggregateType);

}
