package com.dreweaster.jester.example.infrastructure;

import com.dreweaster.jester.application.commandhandler.deduplicating.CommandDeduplicationStrategyFactory;
import com.dreweaster.jester.application.commandhandler.CommandHandlerFactory;
import com.dreweaster.jester.application.commandhandler.deduplicating.DeduplicatingCommandHandlerFactory;
import com.dreweaster.jester.application.commandhandler.deduplicating.TwentyFourHourWindowCommandDeduplicationStrategyFactory;
import com.dreweaster.jester.domain.AggregateId;
import com.dreweaster.jester.domain.CommandId;
import com.dreweaster.jester.infrastructure.eventstore.DummyEventStore;
import com.dreweaster.jester.application.eventstore.EventStore;
import com.dreweaster.jester.example.application.UserService;
import com.dreweaster.jester.example.domain.RegisterUser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.BiConsumer;

/**
 */
public class ExampleEventHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(ExampleEventHandler.class);

    public static void main(String[] args) throws Exception {
        ExampleEventHandler handler = new ExampleEventHandler();
        handler.handleEvent();
    }

    public void handleEvent() throws Exception {

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

        CommandDeduplicationStrategyFactory commandDeduplicationStrategyFactory =
                new TwentyFourHourWindowCommandDeduplicationStrategyFactory();

        CommandHandlerFactory commandHandlerFactory = new DeduplicatingCommandHandlerFactory(
                eventStore,
                commandDeduplicationStrategyFactory);

        UserService userService = new UserService(new CommandHandlerUserRepository(commandHandlerFactory));

        userService.createUser(
                AggregateId.of("deterministic-aggregate-id-1"),
                CommandId.of("deterministic-command-id-1"),
                RegisterUser.of("user1", "password1")
        ).whenComplete(new ResponseHandler());

        userService.createUser(
                AggregateId.of("deterministic-aggregate-id-1"),
                CommandId.of("deterministic-command-id-2"),
                RegisterUser.of("user2", "password2")
        ).whenComplete(new ResponseHandler());

        userService.createUser(
                AggregateId.of("deterministic-aggregate-id-1"),
                CommandId.of("deterministic-command-id-1"),
                RegisterUser.of("user1", "password1")
        ).whenComplete(new ResponseHandler());
    }

    private class ResponseHandler implements BiConsumer<AggregateId, Throwable> {

        @Override
        public void accept(AggregateId aggregateId, Throwable throwable) {
            if (throwable != null) {
                LOGGER.error("Error creating User!", throwable);
            } else {
                LOGGER.info("Created new User with id: " + aggregateId.get());
            }
        }
    }
}
