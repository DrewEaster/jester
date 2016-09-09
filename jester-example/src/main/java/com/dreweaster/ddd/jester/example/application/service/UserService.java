package com.dreweaster.ddd.jester.example.application.service;


import com.dreweaster.ddd.jester.domain.AggregateId;
import com.dreweaster.ddd.jester.domain.CommandId;
import javaslang.concurrent.Future;
import com.dreweaster.ddd.jester.example.domain.aggregates.user.commands.RegisterUser;

/**
 */
public interface UserService {

    Future<AggregateId> createUser(AggregateId aggregateId, CommandId commandId, RegisterUser registerUser);
}
