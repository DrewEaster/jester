package com.dreweaster.jester.example.domain.aggregates.user.commands;

import com.dreweaster.jester.example.domain.util.DomainStyle;
import org.immutables.value.Value;

@Value.Immutable
@DomainStyle
public abstract class AbstractChangeUsername implements UserCommand {

    abstract String getUsername();
}
