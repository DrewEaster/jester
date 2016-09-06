package com.dreweaster.jester.example.application.service;

import com.dreweaster.jester.domain.AggregateId;
import com.dreweaster.jester.domain.CommandId;
import com.dreweaster.jester.example.domain.aggregates.user.commands.RegisterUser;
import javaslang.concurrent.Future;

/**
 */
public interface UserService {

    Future<AggregateId> createUser(AggregateId aggregateId, CommandId commandId, RegisterUser registerUser);
}
