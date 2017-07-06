package com.dreweaster.ddd.jester.example.infrastructure.serialisation;

import com.dreweaster.ddd.jester.infrastructure.driven.eventstore.mapper.json.StatePayloadJsonSerialiser;
import com.dreweaster.ddd.jester.example.domain.aggregates.user.User;
import com.dreweaster.ddd.jester.example.domain.aggregates.user.UserState;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 */
public class UserStateSerialiser extends StatePayloadJsonSerialiser<User, UserState> {

    @Override
    public Class<UserState> stateClass() {
        return UserState.class;
    }

    @Override
    public void doSerialise(UserState userState, ObjectNode root) {
        root.put("username",userState.username());
        root.put("password", userState.username());
        root.put("failed_login_attempts", userState.failedLoginAttempts());
    }
}
