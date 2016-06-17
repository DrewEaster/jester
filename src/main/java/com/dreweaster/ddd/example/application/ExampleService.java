package com.dreweaster.ddd.example.application;

import com.dreweaster.ddd.example.domain.CreateExample;
import com.dreweaster.ddd.example.domain.ExampleAggregate;
import com.dreweaster.ddd.framework.AggregateId;
import com.dreweaster.ddd.framework.CommandEnvelope;
import com.dreweaster.ddd.framework.CommandHandlerFactory;

import java.util.concurrent.CompletionStage;

public class ExampleService {

    private CommandHandlerFactory commandHandlerFactory;

    public ExampleService(CommandHandlerFactory commandHandlerFactory) {
        this.commandHandlerFactory = commandHandlerFactory;
    }

    public CompletionStage<AggregateId> createExample(CommandEnvelope<CreateExample> commandEnvelope) {
        return commandHandlerFactory.handlerFor(ExampleAggregate.class)
                .handle(commandEnvelope)
                .thenApply(events -> commandEnvelope.aggregateId());
    }
}
