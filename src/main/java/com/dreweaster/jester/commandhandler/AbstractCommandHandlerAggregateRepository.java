package com.dreweaster.jester.commandhandler;

import com.dreweaster.jester.domain.Aggregate;
import com.dreweaster.jester.domain.AggregateId;
import com.dreweaster.jester.domain.AggregateRepository;
import com.dreweaster.jester.domain.CommandId;
import com.dreweaster.jester.domain.DomainCommand;
import com.dreweaster.jester.domain.DomainEvent;
import com.dreweaster.jester.eventstore.PersistedEvent;

import java.util.List;
import java.util.concurrent.CompletionStage;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

public abstract class AbstractCommandHandlerAggregateRepository<A extends Aggregate<C, E, State>, C extends DomainCommand, E extends DomainEvent, State> implements AggregateRepository<A, C, E, State> {

    private Class<A> aggregateType;

    private CommandHandlerFactory commandHandlerFactory;

    public AbstractCommandHandlerAggregateRepository(Class<A> aggregateType, CommandHandlerFactory commandHandlerFactory) {
        this.aggregateType = aggregateType;
        this.commandHandlerFactory = commandHandlerFactory;
    }

    @Override
    public final BiFunction<CommandId, ? super C, CompletionStage<List<? super E>>> aggregateRootOf(
            AggregateId aggregateId) {
        return (commandId, command) -> commandHandlerFactory.handlerFor(aggregateType).handle(
                CommandEnvelope.of(
                        aggregateId,
                        commandId,
                        command)
        ).thenApply(events -> events.stream().map(PersistedEvent::rawEvent).collect(Collectors.toList()));
    }
}
