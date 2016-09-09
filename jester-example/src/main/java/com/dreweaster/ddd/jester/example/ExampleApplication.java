package com.dreweaster.ddd.jester.example;

import com.dreweaster.ddd.jester.domain.AggregateId;
import com.dreweaster.ddd.jester.domain.CommandId;
import com.dreweaster.ddd.jester.example.application.service.UserService;
import com.dreweaster.ddd.jester.example.infrastructure.ExampleModule;
import com.google.common.util.concurrent.RateLimiter;
import com.google.inject.Guice;
import com.google.inject.Injector;
import javaslang.concurrent.Future;
import javaslang.control.Try;
import org.flywaydb.core.Flyway;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import com.dreweaster.ddd.jester.example.domain.aggregates.user.events.*;
import com.dreweaster.ddd.jester.example.domain.aggregates.user.commands.*;
import com.dreweaster.ddd.jester.example.domain.aggregates.user.UserState;

// TODO: Refactor into separate child maven module
public class ExampleApplication {

    private static final Logger LOGGER = LoggerFactory.getLogger(ExampleApplication.class);

    public static void main(String[] args) throws Exception {

        migrateDb();

        ExampleApplication application = new ExampleApplication();
        application.run(Guice.createInjector(new ExampleModule()));

        // Shutdown the executor service used by default in Javaslang as part of handling Future callbacks
        Future.DEFAULT_EXECUTOR_SERVICE.shutdownNow();
    }

    public static void migrateDb() throws Exception {
        Flyway flyway = new Flyway();
        flyway.setDataSource("jdbc:postgresql://localhost/postgres", "postgres", "password");
        flyway.migrate();
    }

    public void run(Injector injector) throws Exception {

        UserService userService = injector.getInstance(UserService.class);

        RateLimiter rateLimiter = RateLimiter.create(1000);

        ReportingResponseHandler responseHandler = new ReportingResponseHandler();

        int count = 10000;
        for (int i = 0; i < count; i++) {
            rateLimiter.acquire();
            // Register user for first time
            userService.createUser(
                    AggregateId.of("deterministic-aggregate-id-" + i),
                    CommandId.of("deterministic-command-id-" + i),
                    RegisterUser.builder()
                            .username("user" + i)
                            .password("password")
                            .create()
            ).onComplete(responseHandler);
        }
        //long executionTime = System.currentTimeMillis() - startTime;
        //long executionTimeSeconds = executionTime / 1000;
        //LOGGER.info("Executed in " + executionTime + "ms (" + (count / executionTimeSeconds) + "tps)");


        /*// Send same command with different command id
        userService.createUser(AggregateRoutingCommandEnvelopeWrapper.of(
                        AggregateId.of("deterministic-aggregate-id-1"),
                        CommandId.of("deterministic-command-id-2"),
                        RegisterUser.builder()
                                .username("user2")
                                .password("password2")
                                .create())
        ).onComplete(new ResponseHandler()).await();

        // Send same command with duplicate command id
        userService.createUser(AggregateRoutingCommandEnvelopeWrapper.of(
                        AggregateId.of("deterministic-aggregate-id-1"),
                        CommandId.of("deterministic-command-id-1"),
                        RegisterUser.builder()
                                .username("user1")
                                .password("password1")
                                .create())
        ).onComplete(new ResponseHandler()).await();*/
    }

    private class ReportingResponseHandler implements Consumer<Try<AggregateId>> {

        private long startTime = System.currentTimeMillis();

        private AtomicInteger count = new AtomicInteger();

        public ReportingResponseHandler() {

            ScheduledExecutorService executor = new ScheduledThreadPoolExecutor(1);

            Runnable reporterTask = () -> {
                long executionTimeInSeconds = (System.currentTimeMillis() - startTime) / 1000;
                long tps = count.get() / executionTimeInSeconds;
                LOGGER.info(tps + "tps");
            };

            executor.scheduleAtFixedRate(reporterTask, 1000, 1000, TimeUnit.MILLISECONDS);
        }

        @Override
        public void accept(Try<AggregateId> aggregateIds) {
            aggregateIds
                    .onSuccess(aggregateId -> LOGGER.info("count = " + count.incrementAndGet()))
                    .onFailure(throwable -> LOGGER.error("ERROR!", throwable));
        }
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
