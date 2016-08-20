package com.dreweaster.jester.example.domain.aggregates.user.events;

import com.dreweaster.jester.example.domain.util.DomainStyle;
import org.immutables.value.Value;

@Value.Immutable(singleton = true)
@DomainStyle
public abstract class AbstractUserLocked implements UserEvent {

}