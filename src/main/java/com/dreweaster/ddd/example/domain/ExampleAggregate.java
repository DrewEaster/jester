package com.dreweaster.ddd.example.domain;

import com.dreweaster.ddd.framework.Aggregate;
import com.dreweaster.ddd.framework.Behaviour;
import com.dreweaster.ddd.framework.BehaviourBuilder;

import java.util.Optional;

public class ExampleAggregate extends Aggregate<ExampleEvent, Example> {

    @Override
    protected Behaviour initialBehaviour(Optional<Example> snapshotState) {

        BehaviourBuilder<ExampleEvent, Example> behaviourBuilder = newBehaviourBuilder();

        behaviourBuilder.setReadOnlyCommandHandler(GetExample.class, (cmd, ctx) -> ctx.success(new Example()));

        return behaviourBuilder.build();
    }
}
