package com.dreweaster.jester.example;

import com.dreweaster.jester.domain.AggregateId;
import com.dreweaster.jester.domain.CommandId;
import com.dreweaster.jester.example.application.CommandEnvelope;
import com.dreweaster.jester.example.application.service.UserService;
import com.dreweaster.jester.example.domain.RegisterUser;
import com.dreweaster.jester.example.infrastructure.ExampleModule;
import com.google.inject.Guice;
import com.google.inject.Injector;
import javaslang.concurrent.Future;
import javaslang.control.Try;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Consumer;

public class ExampleApplication {

    private static final Logger LOGGER = LoggerFactory.getLogger(ExampleApplication.class);

    public static void main(String[] args) throws Exception {
        ExampleApplication application = new ExampleApplication();
        application.run(Guice.createInjector(new ExampleModule()));

        // Shutdown the executor service used by default in Javaslang as part of handling Future callbacks
        Future.DEFAULT_EXECUTOR_SERVICE.shutdownNow();
    }

    public void run(Injector injector) throws Exception {

        UserService userService = injector.getInstance(UserService.class);

        // Register user for first time
        userService.createUser(CommandEnvelope.of(
                        AggregateId.of("deterministic-aggregate-id-1"),
                        CommandId.of("deterministic-command-id-1"),
                        RegisterUser.of("user1", "password1"))
        ).onComplete(new ResponseHandler()).await();

        // Send same command with different command id
        userService.createUser(CommandEnvelope.of(
                        AggregateId.of("deterministic-aggregate-id-1"),
                        CommandId.of("deterministic-command-id-2"),
                        RegisterUser.of("user2", "password2"))
        ).onComplete(new ResponseHandler()).await();

        // Send same command with duplicate command id
        userService.createUser(CommandEnvelope.of(
                        AggregateId.of("deterministic-aggregate-id-1"),
                        CommandId.of("deterministic-command-id-1"),
                        RegisterUser.of("user1", "password1"))
        ).onComplete(new ResponseHandler()).await();
    }

    private class ResponseHandler implements Consumer<Try<AggregateId>> {
        @Override
        public void accept(Try<AggregateId> aggregateId) {
            aggregateId
                    .onSuccess(aggregateId1 -> LOGGER.info("Created new User with id: " + aggregateId.get()))
                    .onFailure(throwable -> LOGGER.error("Error creating User!", throwable));
        }
    }
}
