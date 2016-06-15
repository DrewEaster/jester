package com.dreweaster.ddd.framework;

import java.util.Optional;

public abstract class Aggregate<C extends DomainCommand, E extends DomainEvent, State> {

    public static final class InvalidCommandException extends RuntimeException {

        public InvalidCommandException(String message) {
            super(message);
        }
    }

    protected abstract Behaviour initialBehaviour(Optional<State> snapshotState);

    protected final BehaviourBuilder<C, E, State> newBehaviourBuilder() {
        return new BehaviourBuilder<>();
    }
}
