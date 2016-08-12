package com.dreweaster.jester.example.infrastructure;

import com.dreweaster.jester.application.commandhandler.CommandDeduplicationStrategyFactory;
import com.dreweaster.jester.application.commandhandler.CommandHandlerFactory;
import com.dreweaster.jester.application.commandhandler.DeduplicatingCommandHandlerFactory;
import com.dreweaster.jester.application.commandhandler.TwentyFourHourWindowCommandDeduplicationStrategyFactory;
import com.dreweaster.jester.domain.AggregateId;
import com.dreweaster.jester.domain.CommandId;
import com.dreweaster.jester.infrastructure.eventstore.DummyEventStore;
import com.dreweaster.jester.application.eventstore.EventStore;
import com.dreweaster.jester.application.eventstore.StreamEvent;
import com.dreweaster.jester.example.application.ExampleService;
import com.dreweaster.jester.example.domain.RegisterUser;
import com.dreweaster.jester.example.domain.User;
import com.dreweaster.jester.example.domain.UserEvent;
import org.reactivestreams.Publisher;
import org.reactivestreams.Subscriber;
import org.reactivestreams.Subscription;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;
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

        ExampleService exampleService = new ExampleService(new CommandHandlerUserRepository(commandHandlerFactory));

        exampleService.createExample(
                AggregateId.of("deterministic-aggregate-id-1"),
                CommandId.of("deterministic-command-id-1"),
                RegisterUser.of("user1", "password1")
        ).whenComplete(new ResponseHandler());

        exampleService.createExample(
                AggregateId.of("deterministic-aggregate-id-1"),
                CommandId.of("deterministic-command-id-2"),
                RegisterUser.of("user2", "password2")
        ).whenComplete(new ResponseHandler());

        exampleService.createExample(
                AggregateId.of("deterministic-aggregate-id-1"),
                CommandId.of("deterministic-command-id-1"),
                RegisterUser.of("user1", "password1")
        ).whenComplete(new ResponseHandler());

        Publisher<StreamEvent<User, UserEvent>> stream =
                eventStore.stream(User.class, Optional.<Long>empty());

        // FIXME: This isn't working for whatever reason
        stream.subscribe(new Subscriber<StreamEvent<User, UserEvent>>() {

            @Override
            public void onSubscribe(Subscription subscription) {
                LOGGER.info("Subscribed to stream!");
            }

            @Override
            public void onNext(StreamEvent<User, UserEvent> streamEvent) {
                LOGGER.info(String.format("Received Event (offset=%d): %s", streamEvent.offset(), streamEvent));
            }

            @Override
            public void onError(Throwable throwable) {

            }

            @Override
            public void onComplete() {
                LOGGER.info("Stream complete!");
            }
        });
    }

    private class ResponseHandler implements BiConsumer<AggregateId, Throwable> {

        @Override
        public void accept(AggregateId aggregateId, Throwable throwable) {
            if (throwable != null) {
                LOGGER.error("Error creating example!", throwable);
            } else {
                LOGGER.info("Created new example with id: " + aggregateId.get());
            }
        }
    }
}
