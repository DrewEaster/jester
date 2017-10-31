package com.dreweaster.ddd.jester.example.application.repository;

import com.dreweaster.ddd.jester.application.eventstore.EventStore;
import com.dreweaster.ddd.jester.application.repository.CommandDeduplicatingEventsourcedAggregateRepository;
import com.dreweaster.ddd.jester.application.repository.CommandDeduplicationStrategyFactory;
import com.dreweaster.ddd.jester.example.domain.aggregates.user.User;
import com.dreweaster.ddd.jester.example.domain.aggregates.user.commands.UserCommand;
import com.dreweaster.ddd.jester.example.domain.aggregates.user.events.UserEvent;
import com.dreweaster.ddd.jester.example.domain.aggregates.user.repository.UserRepository;
import com.dreweaster.ddd.jester.example.domain.aggregates.user.UserState;

import javax.inject.Inject;

public class CommandDeduplicatingEventsourcedUserRepository
        extends CommandDeduplicatingEventsourcedAggregateRepository<User, UserCommand, UserEvent, UserState>
        implements UserRepository {

    @Inject
    public CommandDeduplicatingEventsourcedUserRepository(
            EventStore eventStore,
            CommandDeduplicationStrategyFactory commandDeduplicationStrategyFactory) {
        super(User.TYPE, eventStore, commandDeduplicationStrategyFactory);
    }
}
