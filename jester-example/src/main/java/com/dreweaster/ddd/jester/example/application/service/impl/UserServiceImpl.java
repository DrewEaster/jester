package com.dreweaster.ddd.jester.example.application.service.impl;

import com.dreweaster.ddd.jester.domain.AggregateId;
import com.dreweaster.ddd.jester.domain.AggregateRepository;
import com.dreweaster.ddd.jester.domain.CommandId;
import com.dreweaster.ddd.jester.example.application.service.UserService;
import com.dreweaster.ddd.jester.example.domain.aggregates.user.repository.UserRepository;
import javaslang.concurrent.Future;
import com.dreweaster.ddd.jester.example.domain.aggregates.user.events.*;
import com.dreweaster.ddd.jester.example.domain.aggregates.user.commands.*;
import com.dreweaster.ddd.jester.example.domain.aggregates.user.UserState;

import javax.inject.Inject;

// TODO: Error handling (Future.recover?)
public class UserServiceImpl implements UserService {

    private UserRepository userRepository;

    @Inject
    public UserServiceImpl(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public Future<AggregateId> createUser(AggregateId aggregateId, CommandId commandId, RegisterUser registerUser) {
        return userRepository.aggregateRootOf(aggregateId)
                .handle(AggregateRepository.CommandEnvelope.of(commandId, registerUser))
                .map(events -> aggregateId);
    }
}
