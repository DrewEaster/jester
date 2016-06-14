package com.dreweaster.ddd.framework;

import java.util.function.BiConsumer;

public class BehaviourBuilder<Event extends DomainEvent, State> {

    public <I extends CommandEnvelope, O, C extends ReadOnlyDomainCommand<O>> void setReadOnlyCommandHandler(
            Class<C> command, BiConsumer<I, ReadOnlyCommandContext<O>> handler) {

    }

    public Behaviour build() {
        return new Behaviour();
    }
}
