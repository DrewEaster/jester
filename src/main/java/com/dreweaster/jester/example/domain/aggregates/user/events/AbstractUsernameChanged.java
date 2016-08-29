package com.dreweaster.jester.example.domain.aggregates.user.events;

import com.dreweaster.jester.example.domain.util.DomainStyle;
import org.immutables.value.Value;

@Value.Immutable
@DomainStyle
public abstract class AbstractUsernameChanged implements UserEvent {

    abstract String username();
}
