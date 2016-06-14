package com.dreweaster.ddd.example.application;

import com.dreweaster.ddd.example.domain.CreateExample;
import com.dreweaster.ddd.example.domain.Example;
import com.dreweaster.ddd.example.domain.ExampleAggregate;
import com.dreweaster.ddd.example.domain.ExampleEvent;
import com.dreweaster.ddd.example.domain.GetExample;
import com.dreweaster.ddd.framework.CommandEnvelope;
import com.dreweaster.ddd.framework.CommandHandlerFactory;
import com.dreweaster.ddd.framework.PersistedEvent;
import com.dreweaster.ddd.framework.ReadOnlyCommandHandler;

import java.util.List;

public class ExampleService {

    private CommandHandlerFactory commandHandlerFactory;

    public ExampleService(CommandHandlerFactory commandHandlerFactory) {
        this.commandHandlerFactory = commandHandlerFactory;
    }

    public void createExample(CommandEnvelope<CreateExample> commandEnvelope) {

        List<PersistedEvent<ExampleAggregate, ExampleEvent>> events =
                commandHandlerFactory.handlerFor(ExampleAggregate.class).handle(commandEnvelope);
    }

    public Example getExample(CommandEnvelope<GetExample> commandEnvelope) {
        return commandHandlerFactory.readOnlyHandlerFor(ExampleAggregate.class).handle(commandEnvelope);
    }
}
