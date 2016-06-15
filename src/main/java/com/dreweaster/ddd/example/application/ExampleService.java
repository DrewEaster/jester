package com.dreweaster.ddd.example.application;

import com.dreweaster.ddd.example.domain.CreateExample;
import com.dreweaster.ddd.example.domain.ExampleAggregate;
import com.dreweaster.ddd.example.domain.ExampleEvent;
import com.dreweaster.ddd.framework.CommandEnvelope;
import com.dreweaster.ddd.framework.CommandHandlerFactory;
import com.dreweaster.ddd.framework.PersistedEvent;
import rx.Single;

import java.util.List;

public class ExampleService {

    private CommandHandlerFactory commandHandlerFactory;

    public ExampleService(CommandHandlerFactory commandHandlerFactory) {
        this.commandHandlerFactory = commandHandlerFactory;
    }

    public void createExample(CommandEnvelope<CreateExample> commandEnvelope) {

        Single<List<PersistedEvent<ExampleAggregate, ExampleEvent>>> events =
                commandHandlerFactory.handlerFor(ExampleAggregate.class).handle(commandEnvelope);
    }
}
