package com.dreweaster.jester.example.domain.commands;

import com.dreweaster.jester.example.domain.util.UserDomainStyle;
import org.immutables.value.Value;

/**
 */
@Value.Immutable
@UserDomainStyle
public abstract class AbstractRegisterUser implements UserCommand {

    abstract String username();

    abstract String password();
}
