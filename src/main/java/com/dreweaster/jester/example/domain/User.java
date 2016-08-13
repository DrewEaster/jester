package com.dreweaster.jester.example.domain;

import com.dreweaster.jester.domain.Aggregate;
import com.dreweaster.jester.domain.Behaviour;
import com.dreweaster.jester.domain.BehaviourBuilder;

// TODO: Deal with snapshots when implemented
public class User extends Aggregate<UserCommand, UserEvent, UserState> {

    public static class AlreadyCreated extends RuntimeException {
        public AlreadyCreated() {
            super("User has already been created!");
        }
    }

    @Override
    public Behaviour<UserCommand, UserEvent, UserState> initialBehaviour() {
        return preCreatedBehaviour();
    }

    /**
     * This is the pre-created behaviour, prior to any commands being handled
     *
     * @return the uncreated behaviour
     */
    public Behaviour<UserCommand, UserEvent, UserState> preCreatedBehaviour() {

        BehaviourBuilder<UserCommand, UserEvent, UserState> behaviourBuilder =
                newBehaviourBuilder(UserState.EMPTY);

        behaviourBuilder.setCommandHandler(RegisterUser.class, (cmd, ctx) ->
                ctx.success(UserRegistered.of(
                        cmd.getUsername(),
                        cmd.getPassword())));

        behaviourBuilder.setEventHandler(UserRegistered.class, (evt, currentBehaviour) ->
                createdBehaviour(UserState.of(
                        evt.getUsername(),
                        evt.getPassword())));

        return behaviourBuilder.build();
    }

    /**
     * This is the post-created behaviour
     *
     * @param state the state of the aggregate
     * @return the post-created behaviour
     */
    public Behaviour<UserCommand, UserEvent, UserState> createdBehaviour(UserState state) {
        BehaviourBuilder<UserCommand, UserEvent, UserState> behaviourBuilder =
                newBehaviourBuilder(state);

        behaviourBuilder.setCommandHandler(RegisterUser.class, (cmd, ctx) -> ctx.error(new AlreadyCreated()));

        return behaviourBuilder.build();
    }
}
