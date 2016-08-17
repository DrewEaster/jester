package com.dreweaster.jester.example.infrastructure;

import com.dreweaster.jester.application.eventstore.EventStore;
import com.dreweaster.jester.application.repository.deduplicating.CommandDeduplicationStrategyFactory;
import com.dreweaster.jester.application.repository.deduplicating.TwentyFourHourWindowCommandDeduplicationStrategyFactory;
import com.dreweaster.jester.example.application.repository.CommandDeduplicatingEventsourcedUserRepository;
import com.dreweaster.jester.example.application.service.UserService;
import com.dreweaster.jester.example.application.service.impl.UserServiceImpl;
import com.dreweaster.jester.example.domain.UserRepository;
import com.dreweaster.jester.infrastructure.eventstore.driven.dummy.DummyEventStore;
import com.google.inject.AbstractModule;

/**
 */
public class ExampleModule extends AbstractModule {

    @Override
    protected void configure() {
        bind(EventStore.class).to(DummyEventStore.class);
        bind(CommandDeduplicationStrategyFactory.class).to(TwentyFourHourWindowCommandDeduplicationStrategyFactory.class);
        bind(UserRepository.class).to(CommandDeduplicatingEventsourcedUserRepository.class);
        bind(UserService.class).to(UserServiceImpl.class);
    }
}
