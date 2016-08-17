package com.dreweaster.jester.example.application.repository;

import com.dreweaster.jester.application.eventstore.EventStore;
import com.dreweaster.jester.application.repository.deduplicating.CommandDeduplicationStrategyFactory;
import com.dreweaster.jester.application.repository.deduplicating.CommandDeduplicatingEventsourcedAggregateRepository;
import com.dreweaster.jester.example.domain.User;
import com.dreweaster.jester.example.domain.UserCommand;
import com.dreweaster.jester.example.domain.UserEvent;
import com.dreweaster.jester.example.domain.UserRepository;
import com.dreweaster.jester.example.domain.UserState;

import javax.inject.Inject;

public class CommandDeduplicatingEventsourcedUserRepository
        extends CommandDeduplicatingEventsourcedAggregateRepository<User, UserCommand, UserEvent, UserState>
        implements UserRepository {

    @Inject
    public CommandDeduplicatingEventsourcedUserRepository(
            EventStore eventStore,
            CommandDeduplicationStrategyFactory commandDeduplicationStrategyFactory) {
        super(User.class, eventStore, commandDeduplicationStrategyFactory);
    }
}
