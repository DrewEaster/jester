package com.dreweaster.ddd.framework;

import java.util.Optional;

public abstract class Aggregate<E extends DomainEvent, State> {

    protected abstract Behaviour initialBehaviour(Optional<State> snapshotState);

    protected final BehaviourBuilder<E, State> newBehaviourBuilder() {
        return new BehaviourBuilder<>();
    }
}
