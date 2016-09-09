package com.dreweaster.ddd.jester.example.domain.aggregates.user;

import com.dreweaster.ddd.jester.example.domain.util.DomainStyle;
import org.immutables.value.Value;

@Value.Immutable
@DomainStyle
public abstract class AbstractUserState {

    abstract String username();

    abstract String password();

    abstract int failedLoginAttempts();
}