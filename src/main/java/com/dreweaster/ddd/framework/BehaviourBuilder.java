package com.dreweaster.ddd.framework;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;

public class BehaviourBuilder<C extends DomainCommand, E extends DomainEvent, State> {

    private Map<Class<? extends C>, BiConsumer<CommandEnvelope<? extends C>, CommandContext<E>>> commandHandlers = new HashMap<>();

    @SuppressWarnings("unchecked")
    public BehaviourBuilder<C, E, State> setCommandHandler(
            Class<? extends C> commandClass, BiConsumer<CommandEnvelope<? extends C>,
            CommandContext<E>> handler) {

        commandHandlers.put(commandClass, handler);
        return this;
    }

    public Behaviour build() {
        return new Behaviour();
    }
}
