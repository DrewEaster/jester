package com.dreweaster.ddd.jester.example.domain.aggregates.user.events;

import com.dreweaster.ddd.jester.example.domain.util.DomainStyle;
import org.immutables.value.Value;

@Value.Immutable
@DomainStyle
public abstract class AbstractPasswordChanged implements UserEvent {

    abstract String password();

    abstract String oldPassword();
}
