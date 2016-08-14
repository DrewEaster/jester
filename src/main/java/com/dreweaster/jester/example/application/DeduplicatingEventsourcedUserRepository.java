package com.dreweaster.jester.example.application;

import com.dreweaster.jester.application.eventstore.EventStore;
import com.dreweaster.jester.application.repository.deduplicating.CommandDeduplicationStrategyFactory;
import com.dreweaster.jester.application.repository.deduplicating.DeduplicatingEventsourcedAggregateRepository;
import com.dreweaster.jester.example.domain.User;
import com.dreweaster.jester.example.domain.UserCommand;
import com.dreweaster.jester.example.domain.UserEvent;
import com.dreweaster.jester.example.domain.UserRepository;
import com.dreweaster.jester.example.domain.UserState;

public class DeduplicatingEventsourcedUserRepository
        extends DeduplicatingEventsourcedAggregateRepository<User, UserCommand, UserEvent, UserState>
        implements UserRepository {

    public DeduplicatingEventsourcedUserRepository(
            EventStore eventStore,
            CommandDeduplicationStrategyFactory commandDeduplicationStrategyFactory) {
        super(User.class, eventStore, commandDeduplicationStrategyFactory);
    }
}
