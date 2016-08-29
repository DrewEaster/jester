package com.dreweaster.jester.example.infrastructure;

import com.dreweaster.jester.application.eventstore.EventStore;
import com.dreweaster.jester.application.repository.deduplicating.CommandDeduplicationStrategyFactory;
import com.dreweaster.jester.application.repository.deduplicating.TwentyFourHourWindowCommandDeduplicationStrategyFactory;
import com.dreweaster.jester.example.application.repository.CommandDeduplicatingEventsourcedUserRepository;
import com.dreweaster.jester.example.application.service.UserService;
import com.dreweaster.jester.example.application.service.impl.UserServiceImpl;
import com.dreweaster.jester.example.domain.aggregates.user.events.*;
import com.dreweaster.jester.example.domain.aggregates.user.repository.UserRepository;
import com.dreweaster.jester.example.infrastructure.mapper.*;
import com.dreweaster.jester.infrastructure.driven.eventstore.serialiser.json.JacksonEventPayloadSerialiser;
import com.dreweaster.jester.infrastructure.driven.eventstore.postgres.Postgres95EventStore;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.AbstractModule;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 */
public class ExampleModule extends AbstractModule {

    @Override
    protected void configure() {
        bind(EventStore.class).toInstance(createEventStore());
        bind(CommandDeduplicationStrategyFactory.class).to(TwentyFourHourWindowCommandDeduplicationStrategyFactory.class);
        bind(UserRepository.class).to(CommandDeduplicatingEventsourcedUserRepository.class);
        bind(UserService.class).to(UserServiceImpl.class);
    }

    // TODO: Temporary config
    private EventStore createEventStore() {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl("jdbc:postgresql://localhost/postgres");
        config.setUsername("postgres");
        config.setPassword("password");
        config.addDataSourceProperty("cachePrepStmts", "true");
        config.addDataSourceProperty("prepStmtCacheSize", "250");
        config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");

        HikariDataSource ds = new HikariDataSource(config);

        // This is so we don't  rely on Jackson Object<=>JSON - we want to keep serialisation/deserialisation completely
        // outside of the domain layer (Hexagonal Architecture FTW).
        JacksonEventPayloadSerialiser serialiser = new JacksonEventPayloadSerialiser(new ObjectMapper());
        serialiser.register(UserRegistered.class, new UserRegisteredEventMapper());
        serialiser.register(UsernameChanged.class, new UsernameChangedEventMapper());
        serialiser.register(PasswordChanged.class, new PasswordChangedEventMapper());
        serialiser.register(FailedLoginAttemptsIncremented.class, new FailedLoginAttemptsEventMapper());
        serialiser.register(UserLocked.class, new UserLockedEventMapper());

        // TODO: Does this make sense - thread per connection?
        // TODO: Number of threads config should drive connection pool size, not other way around
        // TODO: ExecutorService needs to shutdown
        ExecutorService executorService = Executors.newFixedThreadPool(config.getMaximumPoolSize());

        return new Postgres95EventStore(ds, executorService, serialiser);
    }
}
