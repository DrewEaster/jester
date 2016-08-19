package com.dreweaster.jester.example.domain.events;


import com.dreweaster.jester.example.domain.util.UserDomainStyle;
import org.immutables.value.Value;

@Value.Immutable
@UserDomainStyle
public abstract class AbstractUserRegistered implements UserEvent {

    abstract String username();

    abstract String password();
}
