package com.dreweaster.ddd.jester.example.infrastructure;

import com.dreweaster.ddd.jester.domain.DomainModel;
import com.dreweaster.ddd.jester.application.eventstore.EventStore;
import com.dreweaster.ddd.jester.application.repository.deduplicating.CommandDeduplicationStrategyFactory;
import com.dreweaster.ddd.jester.application.repository.deduplicating.TwentyFourHourWindowCommandDeduplicationStrategyFactory;
import com.dreweaster.ddd.jester.example.application.repository.CommandDeduplicatingEventsourcedUserRepository;
import com.dreweaster.ddd.jester.example.application.service.UserService;
import com.dreweaster.ddd.jester.example.application.service.impl.UserServiceImpl;
import com.dreweaster.ddd.jester.example.domain.aggregates.user.User;
import com.dreweaster.ddd.jester.example.domain.aggregates.user.repository.UserRepository;
import com.dreweaster.ddd.jester.example.infrastructure.serialisation.*;
import com.dreweaster.ddd.jester.infrastructure.driven.eventstore.mapper.json.JsonPayloadMapper;
import com.dreweaster.ddd.jester.infrastructure.driven.eventstore.postgres.Postgres95EventStore;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.inject.AbstractModule;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import javaslang.collection.List;

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

    @SuppressWarnings("unchecked")
    // TODO: Need to think through this properly
    private DomainModel createDomainModel() {
        return DomainModel.of(User.TYPE);
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
        config.setAutoCommit(false);

        HikariDataSource ds = new HikariDataSource(config);

        // This is so we don't rely on Jackson Object<=>JSON - we want to keep serialisation/deserialisation completely
        // outside of the domain layer (Hexagonal Architecture FTW).
        JsonPayloadMapper payloadMapper = new JsonPayloadMapper(
                new ObjectMapper(),
                List.of(
                        new UserRegisteredEventMappingConfigurer(),
                        new UsernameChangedEventMappingConfigurer(),
                        new PasswordChangedEventMappingConfigurer(),
                        new FailedLoginAttemptsEventMappingConfigurer(),
                        new UserLockedEventMappingConfigurer()
                ),
                List.of(
                        new UserStateSerialiser()
                ));

        // TODO: Number of threads config should drive connection pool size, not other way around
        // TODO: ExecutorService needs to shutdown
        ExecutorService executorService = Executors.newFixedThreadPool(config.getMaximumPoolSize());

        return new Postgres95EventStore(ds, executorService, payloadMapper, createDomainModel());
    }
}
