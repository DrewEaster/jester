package com.dreweaster.ddd.example.domain;

import com.dreweaster.ddd.framework.Aggregate;
import com.dreweaster.ddd.framework.Behaviour;
import com.dreweaster.ddd.framework.BehaviourBuilder;

// TODO: Deal with snapshots when implemented
public class ExampleAggregate extends Aggregate<ExampleCommand, ExampleEvent, Example> {

    public static class AlreadyCreated extends RuntimeException {
    }

    @Override
    protected Behaviour<ExampleCommand, ExampleEvent, Example> initialBehaviour() {
        return preCreationBehaviour();
    }

    /**
     * This is the pre-created behaviour, prior to any commands being handled
     *
     * @return the uncreated behaviour
     */
    public Behaviour<ExampleCommand, ExampleEvent, Example> preCreationBehaviour() {

        BehaviourBuilder<ExampleCommand, ExampleEvent, Example> behaviourBuilder =
                newBehaviourBuilder(Example.EMPTY_STATE);

        behaviourBuilder.setCommandHandler(CreateExample.class, (cmd, ctx) ->
                ctx.success(ExampleCreated.of(cmd.exampleString())));

        behaviourBuilder.setEventHandler(ExampleCreated.class, (evt, currentBehaviour) ->
                createdBehaviour(Example.of(evt.exampleString())));

        return behaviourBuilder.build();
    }

    /**
     * This is the post-created behaviour
     *
     * @param state the state of the aggregate
     * @return the post-created behaviour
     */
    public Behaviour<ExampleCommand, ExampleEvent, Example> createdBehaviour(Example state) {
        BehaviourBuilder<ExampleCommand, ExampleEvent, Example> behaviourBuilder =
                newBehaviourBuilder(state);

        behaviourBuilder.setCommandHandler(CreateExample.class, (cmd, ctx)
                -> ctx.error(new AlreadyCreated()));

        return behaviourBuilder.build();
    }
}
