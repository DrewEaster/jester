package com.dreweaster.jester.example.application.repository;

import com.dreweaster.jester.application.eventstore.EventStore;
import com.dreweaster.jester.application.repository.deduplicating.CommandDeduplicationStrategyFactory;
import com.dreweaster.jester.application.repository.deduplicating.CommandDeduplicatingEventsourcedAggregateRepository;
import com.dreweaster.jester.example.domain.aggregates.User;
import com.dreweaster.jester.example.domain.aggregates.UserState;
import com.dreweaster.jester.example.domain.commands.UserCommand;
import com.dreweaster.jester.example.domain.events.UserEvent;
import com.dreweaster.jester.example.domain.repository.UserRepository;
import com.dreweaster.jester.example.domain.aggregates.AbstractUserState;

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
