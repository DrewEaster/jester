package com.dreweaster.jester.example.domain.aggregates.user.repository;


import com.dreweaster.jester.domain.AggregateRepository;
import com.dreweaster.jester.example.domain.aggregates.user.User;
import com.dreweaster.jester.example.domain.aggregates.user.UserState;
import com.dreweaster.jester.example.domain.aggregates.user.commands.UserCommand;
import com.dreweaster.jester.example.domain.aggregates.user.events.UserEvent;

public interface UserRepository extends AggregateRepository<User, UserCommand, UserEvent, UserState> {

}
