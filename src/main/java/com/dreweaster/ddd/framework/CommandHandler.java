package com.dreweaster.ddd.framework;

import java.util.List;

public interface CommandHandler<A extends Aggregate<E, ?>, E extends DomainEvent> {

    <C extends DomainCommand> List<PersistedEvent<A, E>> handle(CommandEnvelope<C> command);
}
