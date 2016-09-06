package com.dreweaster.jester.example.application.service.impl;

import com.dreweaster.jester.domain.AggregateRepository;
import com.dreweaster.jester.domain.CommandId;
import com.dreweaster.jester.example.application.service.UserService;
import com.dreweaster.jester.domain.AggregateId;
import com.dreweaster.jester.example.domain.aggregates.user.commands.RegisterUser;
import com.dreweaster.jester.example.domain.aggregates.user.repository.UserRepository;
import javaslang.concurrent.Future;

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
