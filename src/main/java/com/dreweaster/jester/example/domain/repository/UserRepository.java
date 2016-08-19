package com.dreweaster.jester.example.domain.repository;

import com.dreweaster.jester.domain.AggregateRepository;
import com.dreweaster.jester.example.domain.aggregates.AbstractUserState;
import com.dreweaster.jester.example.domain.aggregates.User;
import com.dreweaster.jester.example.domain.aggregates.UserState;
import com.dreweaster.jester.example.domain.commands.UserCommand;
import com.dreweaster.jester.example.domain.events.UserEvent;

public interface UserRepository extends AggregateRepository<User, UserCommand, UserEvent, UserState> {

}
