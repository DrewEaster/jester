package com.dreweaster.jester.application.commandhandler;

import com.dreweaster.jester.domain.Aggregate;
import com.dreweaster.jester.domain.DomainCommand;
import com.dreweaster.jester.domain.DomainEvent;

/**
 */
public interface CommandHandlerFactory {

    <A extends Aggregate<C, E, State>, C extends DomainCommand, E extends DomainEvent, State> CommandHandler<A, C, E, State> handlerFor(Class<A> aggregateType);

}
