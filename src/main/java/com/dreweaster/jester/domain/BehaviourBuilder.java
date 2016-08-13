package com.dreweaster.jester.domain;

import javaslang.control.Either;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;

public class BehaviourBuilder<C extends DomainCommand, E extends DomainEvent, State> {

    private Map<Class<? extends C>, BiFunction<? extends C, CommandContext<E, State>, Either<Throwable, List<E>>>> commandHandlers = new HashMap<>();

    private Map<Class<? extends E>, BiFunction<? extends E, Behaviour<C, E, State>, Behaviour<C, E, State>>> eventHandlers = new HashMap<>();

    private State state;

    public BehaviourBuilder(State state) {
        this.state = state;
    }

    public BehaviourBuilder(
            State state,
            Map<Class<? extends C>, BiFunction<? extends C, CommandContext<E, State>, Either<Throwable, List<E>>>> commandHandlers,
            Map<Class<? extends E>, BiFunction<? extends E, Behaviour<C, E, State>, Behaviour<C, E, State>>> eventHandlers) {
        this.state = state;
        this.commandHandlers = commandHandlers;
        this.eventHandlers = eventHandlers;
    }

    public <Cmd extends C> void setCommandHandler(
            Class<Cmd> commandClass, BiFunction<Cmd,
            CommandContext<E, State>, Either<Throwable, List<E>>> handler) {

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
