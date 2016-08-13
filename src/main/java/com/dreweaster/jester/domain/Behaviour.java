package com.dreweaster.jester.domain;

import javaslang.control.Either;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;

public class Behaviour<C extends DomainCommand, E extends DomainEvent, State> {

    private State state;

    private Map<Class<? extends C>, BiFunction<? extends C, CommandContext<E, State>, Either<Throwable, List<E>>>> commandHandlers = new HashMap<>();

    private Map<Class<? extends E>, BiFunction<? extends E, Behaviour<C, E, State>, Behaviour<C, E, State>>> eventHandlers = new HashMap<>();

    // TODO: Is there some way this can be avoided?
    private Map<Class, BiFunction> untypedCommandHandlers = new HashMap<>();

    // TODO: Is there some way this can be avoided?
    private Map<Class, BiFunction> untypedEventHandlers = new HashMap<>();

    public Behaviour(
            State state,
            Map<Class<? extends C>, BiFunction<? extends C, CommandContext<E, State>, Either<Throwable, List<E>>>> commandHandlers,
            Map<Class<? extends E>, BiFunction<? extends E, Behaviour<C, E, State>, Behaviour<C, E, State>>> eventHandlers) {
        this.state = state;
        this.commandHandlers = commandHandlers;
        this.eventHandlers = eventHandlers;

        // TODO: Nasty hack to overlook my inability to work around the generic type system
        this.untypedCommandHandlers = Collections.unmodifiableMap(commandHandlers);
        this.untypedEventHandlers = Collections.unmodifiableMap(eventHandlers);
    }

    public State state() {
        return state;
    }

    @SuppressWarnings("unchecked")
    public final Either<Throwable, List<E>> handleCommand(C command, CommandContext<E, State> commandContext) {
        BiFunction handler = untypedCommandHandlers.get(command.getClass());
        if (handler != null) {
            return (Either<Throwable, List<E>>) handler.apply(command, commandContext);
        } else {
            // TODO: Think about this error case in more detail
            return Either.left(new IllegalArgumentException("No command handler registered for given command class!"));
        }
    }

    @SuppressWarnings("unchecked")
    public final Behaviour<C, E, State> handleEvent(E event) {
        return (Behaviour<C, E, State>) untypedEventHandlers.get(event.getClass()).apply(event, this);
    }

    /**
     * @return new instance with the given newState
     */
    public Behaviour withState(State newState) {
        return new Behaviour<>(newState, commandHandlers, eventHandlers);
    }

    public BehaviourBuilder<C, E, State> builder() {
        return new BehaviourBuilder<>(state, commandHandlers, eventHandlers);
    }
}