package com.dreweaster.jester.example.domain.aggregates.user.commands;

import com.dreweaster.jester.example.domain.util.DomainStyle;
import org.immutables.value.Value;

@Value.Immutable(singleton = true)
@DomainStyle
public abstract class AbstractIncrementFailedLoginAttempts implements UserCommand {

}
