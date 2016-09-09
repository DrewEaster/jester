package com.dreweaster.ddd.jester.domain;

public abstract class Aggregate<C extends DomainCommand, E extends DomainEvent, State> {

    public static final class InvalidCommandException extends RuntimeException {

        public InvalidCommandException(String message) {
            super(message);
        }
    }

    public abstract Behaviour<C, E, State> initialBehaviour();

    protected final BehaviourBuilder<C, E, State> newBehaviourBuilder(State state) {
        return new BehaviourBuilder<>(state);
    }
}
