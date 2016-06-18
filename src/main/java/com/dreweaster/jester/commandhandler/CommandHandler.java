package com.dreweaster.jester.commandhandler;

import com.dreweaster.jester.domain.Aggregate;
import com.dreweaster.jester.domain.DomainCommand;
import com.dreweaster.jester.domain.DomainEvent;
import com.dreweaster.jester.eventstore.PersistedEvent;

import java.util.List;
import java.util.concurrent.CompletionStage;

public interface CommandHandler<A extends Aggregate<C, E, State>, C extends DomainCommand, E extends DomainEvent, State> {

    CompletionStage<List<PersistedEvent<A, E>>> handle(CommandEnvelope<? extends C> command);
}
