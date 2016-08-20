package com.dreweaster.jester.example.domain.aggregates.user.events;

import com.dreweaster.jester.example.domain.util.DomainStyle;
import org.immutables.value.Value;

@Value.Immutable
@DomainStyle
public abstract class AbstractPasswordChanged implements UserEvent {

    abstract String password();

    abstract String oldPassword();
}
