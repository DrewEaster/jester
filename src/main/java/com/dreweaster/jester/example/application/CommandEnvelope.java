package com.dreweaster.jester.example.application;

import com.dreweaster.jester.domain.AggregateId;
import com.dreweaster.jester.domain.CommandId;
import com.dreweaster.jester.domain.DomainCommand;

/**
 */
public final class CommandEnvelope<C extends DomainCommand> {

    public static <C extends DomainCommand> CommandEnvelope<C> of(AggregateId aggregateId, CommandId commandId, C command) {
        return new CommandEnvelope<>(aggregateId, commandId, command);
    }

    private CommandId commandId;

    private AggregateId aggregateId;

    private C command;

    public CommandEnvelope(AggregateId aggregateId, CommandId commandId, C command) {
        this.aggregateId = aggregateId;
        this.commandId = commandId;
        this.command = command;
    }

    public CommandId commandId() {
        return commandId;
    }

    public AggregateId aggregateId() {
        return aggregateId;
    }

    public C command() {
        return command;
    }
}
