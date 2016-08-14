package com.dreweaster.jester.example.application;

import com.dreweaster.jester.example.domain.RegisterUser;
import com.dreweaster.jester.example.domain.UserRepository;
import com.dreweaster.jester.domain.AggregateId;
import com.dreweaster.jester.domain.CommandId;
import javaslang.concurrent.Future;

import java.util.concurrent.CompletionStage;

public class UserService {

    private UserRepository userRepository;

    public UserService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public Future<AggregateId> createUser(
            AggregateId aggregateId,
            CommandId commandId,
            RegisterUser command) {

        return userRepository.aggregateRootOf(aggregateId)
                .handle(commandId, command)
                .map(events -> aggregateId);
    }
}
