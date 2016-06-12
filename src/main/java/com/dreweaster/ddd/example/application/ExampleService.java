package com.dreweaster.ddd.example.application;

import com.dreweaster.ddd.example.domain.*;
import com.dreweaster.ddd.framework.*;

public class ExampleService {

    private CommandHandlerFactory commandHandlerFactory;

    public ExampleService(CommandHandlerFactory commandHandlerFactory) {
        this.commandHandlerFactory = commandHandlerFactory;
    }

    public void createExample(Command<CreateExample> command) {

        CommandHandler<ExampleAggregate, CreateExample, ExampleEvent> commandHandler =
                commandHandlerFactory.handlerFor(
                        ExampleAggregate.class,
                        command.aggregateId());

        commandHandler.handle(command);
    }

    public Example getExample(Command<GetExample> command) {

        ReadOnlyCommandHandler<GetExample, Example> readOnlyCommandHandler =
                commandHandlerFactory.readOnlyHandlerFor(
                        ExampleAggregate.class,
                        command.aggregateId());

        return readOnlyCommandHandler.handle(command);
    }
}
