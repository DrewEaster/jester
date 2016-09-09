package com.dreweaster.ddd.jester.example.domain.aggregates.user.events;

import com.dreweaster.ddd.jester.example.domain.util.DomainStyle;
import org.immutables.value.Value;

@Value.Immutable(singleton = true)
@DomainStyle
public abstract class AbstractFailedLoginAttemptsIncremented implements UserEvent {

}
