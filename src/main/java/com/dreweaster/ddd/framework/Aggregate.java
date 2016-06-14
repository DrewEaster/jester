package com.dreweaster.ddd.framework;

import java.util.Optional;

public abstract class Aggregate<Event extends DomainEvent, State> {

    protected abstract Behaviour initialBehaviour(Optional<State> snapshotState);

    protected final BehaviourBuilder<Event, State> newBehaviourBuilder() {
        return new BehaviourBuilder<>();
    }
}
