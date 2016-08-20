package com.dreweaster.jester.domain;

import javaslang.Function2;
import javaslang.collection.HashMap;
import javaslang.collection.List;
import javaslang.collection.Map;
import javaslang.control.Either;

public class BehaviourBuilder<C extends DomainCommand, E extends DomainEvent, State> {

    private Map<Class<? extends C>, Function2<? extends C, CommandContext<E, State>, Either<Throwable, List<E>>>> commandHandlers = HashMap.empty();

    private Map<Class<? extends E>, Function2<? extends E, Behaviour<C, E, State>, Behaviour<C, E, State>>> eventHandlers = HashMap.empty();

    private State state;

    public BehaviourBuilder(State state) {
        this.state = state;
    }

    public BehaviourBuilder(
            State state,
            Map<Class<? extends C>, Function2<? extends C, CommandContext<E, State>, Either<Throwable, List<E>>>> commandHandlers,
            Map<Class<? extends E>, Function2<? extends E, Behaviour<C, E, State>, Behaviour<C, E, State>>> eventHandlers) {
        this.state = state;
        this.commandHandlers = commandHandlers;
        this.eventHandlers = eventHandlers;
    }

    public <Cmd extends C> void setCommandHandler(
            Class<Cmd> commandClass, Function2<Cmd,
            CommandContext<E, State>, Either<Throwable, List<E>>> handler) {

        commandHandlers = commandHandlers.put(commandClass, handler);
    }

    public <Evt extends E> void setEventHandler(
            Class<Evt> eventClass,
            Function2<Evt, Behaviour<C, E, State>, Behaviour<C, E, State>> handler) {
        eventHandlers = eventHandlers.put(eventClass, handler);
    }

    public Behaviour<C, E, State> build() {
        return new Behaviour<>(state, commandHandlers, eventHandlers);
    }
}
