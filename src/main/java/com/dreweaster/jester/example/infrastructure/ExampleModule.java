package com.dreweaster.jester.example.infrastructure;

import com.dreweaster.jester.application.eventstore.EventStore;
import com.dreweaster.jester.application.repository.deduplicating.CommandDeduplicationStrategyFactory;
import com.dreweaster.jester.application.repository.deduplicating.TwentyFourHourWindowCommandDeduplicationStrategyFactory;
import com.dreweaster.jester.example.application.repository.CommandDeduplicatingEventsourcedUserRepository;
import com.dreweaster.jester.example.application.service.UserService;
import com.dreweaster.jester.example.application.service.impl.UserServiceImpl;
import com.dreweaster.jester.example.domain.aggregates.user.repository.UserRepository;
import com.dreweaster.jester.infrastructure.eventstore.driven.JacksonMapper;
import com.dreweaster.jester.infrastructure.eventstore.driven.memory.InMemoryEventStore;
import com.dreweaster.jester.infrastructure.eventstore.driven.postgres.Postgres95EventStore;
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
        JacksonMapper mapper = JacksonMapper.newSnakeCaseObjectMapper();

        // TODO: Does this make sense - thread per connection?
        // TODO: ExecutorService needs to shutdown
        ExecutorService executorService = Executors.newFixedThreadPool(config.getMaximumPoolSize());

        return new Postgres95EventStore(ds, executorService, mapper);
    }
}
