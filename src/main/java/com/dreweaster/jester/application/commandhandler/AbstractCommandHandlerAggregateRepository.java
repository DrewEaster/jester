package com.dreweaster.jester.application.commandhandler;

import com.dreweaster.jester.application.eventstore.PersistedEvent;
import com.dreweaster.jester.domain.*;

import java.util.stream.Collectors;

public abstract class AbstractCommandHandlerAggregateRepository<A extends Aggregate<C, E, State>, C extends DomainCommand, E extends DomainEvent, State> implements AggregateRepository<A, C, E, State> {

    private Class<A> aggregateType;

    private CommandHandlerFactory commandHandlerFactory;

    public AbstractCommandHandlerAggregateRepository(Class<A> aggregateType, CommandHandlerFactory commandHandlerFactory) {
        this.aggregateType = aggregateType;
        this.commandHandlerFactory = commandHandlerFactory;
    }

    @Override
    public final AggregateRoot<C, E> aggregateRootOf(AggregateId aggregateId) {
        return (commandId, command) -> commandHandlerFactory.handlerFor(aggregateType).handle(
                CommandEnvelope.of(
                        aggregateId,
                        commandId,
                        command)
        ).thenApply(events -> events.stream().map(PersistedEvent::rawEvent).collect(Collectors.toList()));
    }
}
