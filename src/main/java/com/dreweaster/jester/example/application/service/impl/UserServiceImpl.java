package com.dreweaster.jester.example.application.service.impl;

import com.dreweaster.jester.example.application.CommandEnvelope;
import com.dreweaster.jester.example.application.service.UserService;
import com.dreweaster.jester.example.domain.commands.RegisterUser;
import com.dreweaster.jester.example.domain.repository.UserRepository;
import com.dreweaster.jester.domain.AggregateId;
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
    public Future<AggregateId> createUser(CommandEnvelope<RegisterUser> commandEnvelope) {
        return userRepository.aggregateRootOf(commandEnvelope.aggregateId())
                .handle(commandEnvelope.commandId(), commandEnvelope.command())
                .map(events -> commandEnvelope.aggregateId());
    }
}
