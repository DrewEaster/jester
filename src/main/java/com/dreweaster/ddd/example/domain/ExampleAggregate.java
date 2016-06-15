package com.dreweaster.ddd.example.domain;

import com.dreweaster.ddd.framework.Aggregate;
import com.dreweaster.ddd.framework.Behaviour;
import com.dreweaster.ddd.framework.BehaviourBuilder;

import java.util.Optional;

public class ExampleAggregate extends Aggregate<ExampleCommand, ExampleEvent, Example> {

    @Override
    protected Behaviour initialBehaviour(Optional<Example> snapshotState) {

        BehaviourBuilder<ExampleCommand, ExampleEvent, Example> behaviourBuilder = newBehaviourBuilder();

        behaviourBuilder.setCommandHandler(CreateExample.class, (cmd, ctx) -> ctx.success(new ExampleCreated()));

        return behaviourBuilder.build();
    }
}
