package com.dreweaster.ddd.framework;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;

public class Behaviour<C extends DomainCommand, E extends DomainEvent, State> {

    private State state;

    private Map<Class<? extends C>, BiConsumer<? extends C, CommandContext<E, State>>> commandHandlers = new HashMap<>();

    private Map<Class<? extends E>, BiFunction<? extends E, Behaviour<C, E, State>, Behaviour<C, E, State>>> eventHandlers = new HashMap<>();

    private Map<Class, BiConsumer> untypedCommandHandlers = new HashMap<>();

    private Map<Class, BiFunction> untypedEventHandlers = new HashMap<>();

    public Behaviour(
            State state,
            Map<Class<? extends C>, BiConsumer<? extends C, CommandContext<E, State>>> commandHandlers,
            Map<Class<? extends E>, BiFunction<? extends E, Behaviour<C, E, State>, Behaviour<C, E, State>>> eventHandlers) {
        this.state = state;
        this.commandHandlers = commandHandlers;
        this.eventHandlers = eventHandlers;
        this.untypedCommandHandlers = new HashMap<>(commandHandlers);
        this.untypedEventHandlers = new HashMap<>(eventHandlers);
    }

    public State state() {
        return state;
    }

    @SuppressWarnings("unchecked")
    public final boolean handleCommand(C command, CommandContext<E, State> commandContext) {
        BiConsumer handler = untypedCommandHandlers.get(command.getClass());
        if (handler != null) {
            handler.accept(command, commandContext);
            return true;
        } else {
            return false;
        }
    }

    @SuppressWarnings("unchecked")
    public final Behaviour<C, E, State> handleEvent(E event) {
        return (Behaviour<C, E, State>) untypedEventHandlers.get(event.getClass()).apply(event, this);
    }

    /**
     * @return new instance with the given currentState
     */
    public Behaviour withState(State newState) {
        return new Behaviour<>(newState, commandHandlers, eventHandlers);
    }

    public BehaviourBuilder<C, E, State> builder() {
        return new BehaviourBuilder<>(state, commandHandlers, eventHandlers);
    }
}