package com.dreweaster.jester.example.infrastructure;

import com.dreweaster.jester.example.domain.User;
import com.dreweaster.jester.example.domain.UserCommand;
import com.dreweaster.jester.example.domain.UserEvent;
import com.dreweaster.jester.example.domain.UserRepository;
import com.dreweaster.jester.example.domain.UserState;
import com.dreweaster.jester.commandhandler.AbstractCommandHandlerAggregateRepository;
import com.dreweaster.jester.commandhandler.CommandHandlerFactory;

public class CommandHandlerUserRepository
        extends AbstractCommandHandlerAggregateRepository<User, UserCommand, UserEvent, UserState>
        implements UserRepository {

    public CommandHandlerUserRepository(CommandHandlerFactory commandHandlerFactory) {
        super(User.class, commandHandlerFactory);
    }
}
