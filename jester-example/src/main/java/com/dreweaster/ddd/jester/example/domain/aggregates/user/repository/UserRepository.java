package com.dreweaster.ddd.jester.example.domain.aggregates.user.repository;

import com.dreweaster.ddd.jester.domain.AggregateRepository;
import com.dreweaster.ddd.jester.example.domain.aggregates.user.User;
import com.dreweaster.ddd.jester.example.domain.aggregates.user.commands.UserCommand;
import com.dreweaster.ddd.jester.example.domain.aggregates.user.events.UserEvent;
import com.dreweaster.ddd.jester.example.domain.aggregates.user.UserState;

public interface UserRepository extends AggregateRepository<User, UserCommand, UserEvent, UserState> {

}
