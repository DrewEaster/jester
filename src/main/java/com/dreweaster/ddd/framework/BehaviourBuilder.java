package com.dreweaster.ddd.framework;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;

public class BehaviourBuilder<C extends DomainCommand, E extends DomainEvent, State> {

    private Map<Class<? extends C>, BiConsumer<? extends C, CommandContext<E>>> commandHandlers = new HashMap<>();

    private Map<Class<? extends E>, BiFunction<? extends E, Behaviour<C, E, State>, Behaviour<C, E, State>>> eventHandlers = new HashMap<>();

    private State state;

    public BehaviourBuilder(State state) {
        this.state = state;
    }

    public BehaviourBuilder(
            State state,
            Map<Class<? extends C>, BiConsumer<? extends C, CommandContext<E>>> commandHandlers,
            Map<Class<? extends E>, BiFunction<? extends E, Behaviour<C, E, State>, Behaviour<C, E, State>>> eventHandlers) {
        this.state = state;
        this.commandHandlers = commandHandlers;
        this.eventHandlers = eventHandlers;
    }

    public <Cmd extends C> void setCommandHandler(
            Class<Cmd> commandClass, BiConsumer<Cmd,
            CommandContext<E>> handler) {

        commandHandlers.put(commandClass, handler);
    }

    public <Evt extends E> void setEventHandler(
            Class<Evt> eventClass,
            BiFunction<Evt, Behaviour<C, E, State>, Behaviour<C, E, State>> handler) {
        eventHandlers.put(eventClass, handler);
    }

    public Behaviour<C, E, State> build() {
        return new Behaviour<>(state, commandHandlers, eventHandlers);
    }
}
