package com.dreweaster.ddd.framework;

/**
 */
public interface CommandHandlerFactory {

    <A extends Aggregate<C, E, State>, C extends DomainCommand, E extends DomainEvent, State> CommandHandler<A, C, E, State> handlerFor(Class<A> aggregateType);

}
