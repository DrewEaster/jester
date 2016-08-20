package com.dreweaster.jester.example.application.service;

import com.dreweaster.jester.domain.AggregateId;
import com.dreweaster.jester.example.application.CommandEnvelope;
import com.dreweaster.jester.example.domain.aggregates.user.commands.RegisterUser;
import javaslang.concurrent.Future;

/**
 */
public interface UserService {

    Future<AggregateId> createUser(CommandEnvelope<RegisterUser> commandEnvelope);
}
