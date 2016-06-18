package com.dreweaster.jester.example.application;

import com.dreweaster.jester.example.domain.RegisterUser;
import com.dreweaster.jester.example.domain.UserRepository;
import com.dreweaster.jester.domain.AggregateId;
import com.dreweaster.jester.domain.CommandId;

import java.util.concurrent.CompletionStage;

public class ExampleService {

    private UserRepository userRepository;

    public ExampleService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public CompletionStage<AggregateId> createExample(
            AggregateId aggregateId,
            CommandId commandId,
            RegisterUser command) {

        return userRepository.aggregateRootOf(aggregateId)
                .apply(commandId, command)
                .thenApply(events -> aggregateId);
    }
}
