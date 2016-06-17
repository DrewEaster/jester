package com.dreweaster.ddd.example.domain;

import com.dreweaster.ddd.framework.Aggregate;
import com.dreweaster.ddd.framework.Behaviour;
import com.dreweaster.ddd.framework.BehaviourBuilder;

import java.util.Optional;

public class ExampleAggregate extends Aggregate<ExampleCommand, ExampleEvent, Example> {

    public static class AlreadyCreated extends RuntimeException {
    }

    @Override
    protected Behaviour<ExampleCommand, ExampleEvent, Example> initialBehaviour(Optional<Example> snapshotState) {
        // TODO: Deal with snapshots when implemented
        return new UncreatedBehaviourFactory().create();
    }

    /**
     * Factory for creating UncreatedBehaviour
     */
    public class UncreatedBehaviourFactory {

        public Behaviour<ExampleCommand, ExampleEvent, Example> create() {

            BehaviourBuilder<ExampleCommand, ExampleEvent, Example> behaviourBuilder =
                    newBehaviourBuilder(Example.EMPTY_STATE);

            behaviourBuilder.setCommandHandler(CreateExample.class, (cmd, ctx) ->
                    ctx.success(ExampleCreated.of(cmd.exampleString())));

            behaviourBuilder.setEventHandler(ExampleCreated.class, (evt, currentBehaviour) ->
                    new CreatedBehaviourFactory().create(Example.of(evt.exampleString())));

            return behaviourBuilder.build();
        }
    }

    /**
     * Factory for creating CreatedBehaviour
     */
    public class CreatedBehaviourFactory {

        public Behaviour<ExampleCommand, ExampleEvent, Example> create(Example state) {

            BehaviourBuilder<ExampleCommand, ExampleEvent, Example> behaviourBuilder =
                    newBehaviourBuilder(state);

            behaviourBuilder.setCommandHandler(CreateExample.class, (cmd, ctx)
                    -> ctx.error(new AlreadyCreated()));

            return behaviourBuilder.build();
        }
    }
}
