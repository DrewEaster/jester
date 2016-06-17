package com.dreweaster.ddd.example.infrastructure;

import com.dreweaster.ddd.example.application.ExampleService;
import com.dreweaster.ddd.example.domain.CreateExample;
import com.dreweaster.ddd.framework.AggregateId;
import com.dreweaster.ddd.framework.CommandDeduplicationStrategyFactory;
import com.dreweaster.ddd.framework.CommandDeduplicationStrategyFactoryImpl;
import com.dreweaster.ddd.framework.CommandEnvelope;
import com.dreweaster.ddd.framework.CommandHandlerFactory;
import com.dreweaster.ddd.framework.CommandId;
import com.dreweaster.ddd.framework.DeduplicatingCommandHandlerFactory;
import com.dreweaster.ddd.framework.DummyEventStore;
import com.dreweaster.ddd.framework.EventStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.BiConsumer;

/**
 */
public class ExampleEventHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(ExampleEventHandler.class);

    public static void main(String[] args) {
        ExampleEventHandler handler = new ExampleEventHandler();
        handler.handleEvent();
    }

    public void handleEvent() {

        // This code is highly dependent on guaranteed message ordering. This doesn't mean that we can't handle
        // duplicate messages, it just means that we need guarantees that we'll always first see messages in order - e.g. it
        // can't be possible to see message 4 before seeing message 3, but it's okay to see message 3, then message 4,
        // followed by message 3 again.

        // FOR NEW AGGREGATES
        // ******************
        // If being created asynchronously in reaction to an event from elsewhere in the system, we need
        // the aggregate id to be 'deterministic'. This means that for the same event we're reacting to, we'd
        // always create the same aggregate id here. In such a case, the aggregate can react to an initial 'Create' command
        // by responding with an 'Already Created' exception.

        // FOR COMMANDS ON EXISTING AGGREGATES
        // ***********************************
        // If command is being sent asynchronously in reaction to an event from elsewhere in the system, we need
        // the command id to be 'deterministic'. This means that for the same event we're reacting to, we'd
        // always create the same command id here. In such a case, the command handler can recognise that the
        // command has already been handled, and ignore it without forwarding it to the aggregate.

        EventStore eventStore = new DummyEventStore();
        CommandDeduplicationStrategyFactory commandDeduplicationStrategyFactory = new CommandDeduplicationStrategyFactoryImpl();
        CommandHandlerFactory commandHandlerFactory = new DeduplicatingCommandHandlerFactory(
                eventStore,
                commandDeduplicationStrategyFactory);

        ExampleService exampleService = new ExampleService(commandHandlerFactory);

        CommandEnvelope<CreateExample> cmd1 = CommandEnvelope.of(
                AggregateId.of("deterministic-aggregate-id-1"),
                CommandId.of("deterministic-command-id-1"),
                CreateExample.of("Hello, World!"));

        CommandEnvelope<CreateExample> cmd2 = CommandEnvelope.of(
                AggregateId.of("deterministic-aggregate-id-1"),
                CommandId.of("deterministic-command-id-2"),
                CreateExample.of("Another hello, World!"));

        exampleService.createExample(cmd1).whenComplete(new ResponseHandler());
        exampleService.createExample(cmd2).whenComplete(new ResponseHandler());
        exampleService.createExample(cmd1).whenComplete(new ResponseHandler());
    }

    private class ResponseHandler implements BiConsumer<Void, Throwable> {

        @Override
        public void accept(Void aVoid, Throwable throwable) {
            if (throwable != null) {
                LOGGER.error("Error creating example!", throwable);
            }
        }
    }
}
