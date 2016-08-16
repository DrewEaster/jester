package com.dreweaster.jester.domain;

public interface UserRepository extends AggregateRepository<User, UserCommand, UserEvent, UserState> {

}
