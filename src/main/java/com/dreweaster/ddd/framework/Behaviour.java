package com.dreweaster.ddd.framework;

import java.util.HashMap;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;

public class Behaviour<C extends DomainCommand, E extends DomainEvent, State> {

    private State state;

    private Map<Class<? extends C>, BiConsumer<? extends C, CommandContext<E>>> commandHandlers = new HashMap<>();

    private Map<Class<? extends E>, BiFunction<? extends E, Behaviour<C, E, State>, Behaviour<C, E, State>>> eventHandlers = new HashMap<>();

    private Map<Class, BiConsumer> untypedCommandHandlers = new HashMap<>();

    private Map<Class, BiFunction> untypedEventHandlers = new HashMap<>();

    public Behaviour(
            State state,
            Map<Class<? extends C>, BiConsumer<? extends C, CommandContext<E>>> commandHandlers,
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
    public final void handleCommand(C command, CommandContext<E> commandContext) {
        untypedCommandHandlers.get(command.getClass()).accept(command, commandContext);
    }

    @SuppressWarnings("unchecked")
    public final Behaviour<C, E, State> handleEvent(E event) {
        return (Behaviour<C, E, State>) untypedEventHandlers.get(event.getClass()).apply(event, this);
    }

    /**
     * @return new instance with the given state
     */
    public Behaviour withState(State newState) {
        return new Behaviour<>(newState, commandHandlers, eventHandlers);
    }

    public BehaviourBuilder<C, E, State> builder() {
        return new BehaviourBuilder<>(state, commandHandlers, eventHandlers);
    }
}