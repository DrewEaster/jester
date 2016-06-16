package com.dreweaster.ddd.example.application;

import com.dreweaster.ddd.example.domain.CreateExample;
import com.dreweaster.ddd.example.domain.ExampleAggregate;
import com.dreweaster.ddd.example.domain.ExampleEvent;
import com.dreweaster.ddd.framework.CommandEnvelope;
import com.dreweaster.ddd.framework.CommandHandlerFactory;
import com.dreweaster.ddd.framework.PersistedEvent;

import java.util.List;
import java.util.concurrent.CompletionStage;

public class ExampleService {

    private CommandHandlerFactory commandHandlerFactory;

    public ExampleService(CommandHandlerFactory commandHandlerFactory) {
        this.commandHandlerFactory = commandHandlerFactory;
    }

    public void createExample(CommandEnvelope<CreateExample> commandEnvelope) {

        CompletionStage<List<PersistedEvent<ExampleAggregate, ExampleEvent>>> eventsFuture =
                commandHandlerFactory.handlerFor(ExampleAggregate.class).handle(commandEnvelope);

        eventsFuture.whenComplete((events, error) -> {
            if (events != null) {
                events.forEach(System.out::println);
            } else {
                error.printStackTrace();
            }
        });
    }
}
