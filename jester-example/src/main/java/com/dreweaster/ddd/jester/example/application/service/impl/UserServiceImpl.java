package com.dreweaster.ddd.jester.example.application.service.impl;

import com.dreweaster.ddd.jester.domain.AggregateId;
import com.dreweaster.ddd.jester.domain.CommandId;
import com.dreweaster.ddd.jester.example.application.service.UserService;
import com.dreweaster.ddd.jester.example.domain.aggregates.user.repository.UserRepository;
import io.vavr.concurrent.Future;
import com.dreweaster.ddd.jester.example.domain.aggregates.user.commands.RegisterUser;

import javax.inject.Inject;

import static com.dreweaster.ddd.jester.domain.AggregateRepository.*;

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
                .handle(CommandEnvelope.of(commandId, registerUser))
                .map(result -> aggregateId);
//                Match(r.getClass()).of(
//                        Case($(SuccessResult.class), Future.successful(aggregateId)),
//                        Case($(RejectionResult.class), Future.failed(new RuntimeException())),
//                        Case($(ConcurrentModificationResult.class), Future.failed(new EventStore.OptimisticConcurrencyException()))
//                )
    }
}
