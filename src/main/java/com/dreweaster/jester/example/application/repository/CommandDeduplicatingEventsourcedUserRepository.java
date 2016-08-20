package com.dreweaster.jester.example.application.repository;

import com.dreweaster.jester.application.eventstore.EventStore;
import com.dreweaster.jester.application.repository.deduplicating.CommandDeduplicatingEventsourcedAggregateRepository;
import com.dreweaster.jester.application.repository.deduplicating.CommandDeduplicationStrategyFactory;
import com.dreweaster.jester.example.domain.aggregates.user.User;
import com.dreweaster.jester.example.domain.aggregates.user.UserState;
import com.dreweaster.jester.example.domain.aggregates.user.commands.UserCommand;
import com.dreweaster.jester.example.domain.aggregates.user.events.UserEvent;
import com.dreweaster.jester.example.domain.aggregates.user.repository.UserRepository;

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
