package com.dreweaster.ddd.framework;

public abstract class Aggregate<C extends DomainCommand, E extends DomainEvent, State> {

    public static final class InvalidCommandException extends RuntimeException {

        public InvalidCommandException(String message) {
            super(message);
        }
    }

    protected abstract Behaviour<C, E, State> initialBehaviour();

    protected final BehaviourBuilder<C, E, State> newBehaviourBuilder(State state) {
        return new BehaviourBuilder<>(state);
    }
}
