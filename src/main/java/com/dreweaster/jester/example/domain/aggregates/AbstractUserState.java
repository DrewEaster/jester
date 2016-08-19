package com.dreweaster.jester.example.domain.aggregates;

import com.dreweaster.jester.example.domain.util.UserDomainStyle;
import org.immutables.value.Value;

@Value.Immutable
@UserDomainStyle
public abstract class AbstractUserState {

    abstract String username();

    abstract String password();
}