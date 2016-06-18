package com.dreweaster.jester.example.domain;

import com.dreweaster.jester.domain.AggregateRepository;

public interface UserRepository extends AggregateRepository<User, UserCommand, UserEvent, UserState> {

}
