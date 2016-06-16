package com.dreweaster.ddd.example.infrastructure;

import com.dreweaster.ddd.example.application.ExampleService;
import com.dreweaster.ddd.example.domain.CreateExample;
import com.dreweaster.ddd.framework.AggregateId;
import com.dreweaster.ddd.framework.AggregateRootFactory;
import com.dreweaster.ddd.framework.AggregateRootFactoryImpl;
import com.dreweaster.ddd.framework.CommandDeduplicationStrategyFactory;
import com.dreweaster.ddd.framework.CommandDeduplicationStrategyFactoryImpl;
import com.dreweaster.ddd.framework.CommandEnvelope;
import com.dreweaster.ddd.framework.CommandHandlerFactory;
import com.dreweaster.ddd.framework.CommandId;
import com.dreweaster.ddd.framework.DeduplicatingCommandHandlerFactory;
import com.dreweaster.ddd.framework.DummyEventStore;
import com.dreweaster.ddd.framework.EventStore;

/**
 */
public class ExampleEventHandler {

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
        AggregateRootFactory aggregateRootFactory = new AggregateRootFactoryImpl();
        CommandDeduplicationStrategyFactory commandDeduplicationStrategyFactory = new CommandDeduplicationStrategyFactoryImpl();
        CommandHandlerFactory commandHandlerFactory = new DeduplicatingCommandHandlerFactory(
                aggregateRootFactory,
                eventStore,
                commandDeduplicationStrategyFactory);

        ExampleService exampleService = new ExampleService(commandHandlerFactory);

        CommandEnvelope<CreateExample> cmd = CommandEnvelope.of(
                AggregateId.of("deterministic-aggregate-id"),
                CommandId.of("deterministic-command-id"),
                CreateExample.of("Hello, World!"));

        exampleService.createExample(cmd);

        exampleService.createExample(cmd);
    }
}
