package com.dreweaster.jester.example.domain.aggregates;

import com.dreweaster.jester.domain.Aggregate;
import com.dreweaster.jester.domain.Behaviour;
import com.dreweaster.jester.domain.BehaviourBuilder;
import com.dreweaster.jester.example.domain.commands.RegisterUser;
import com.dreweaster.jester.example.domain.commands.UserCommand;
import com.dreweaster.jester.example.domain.events.UserEvent;
import com.dreweaster.jester.example.domain.events.UserRegistered;

// TODO: Deal with snapshots when implemented
public class User extends Aggregate<UserCommand, UserEvent, UserState> {

    private static final UserState EMPTY_STATE = UserState.builder().username("").password("").create();

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
                newBehaviourBuilder(EMPTY_STATE);

        behaviourBuilder.setCommandHandler(RegisterUser.class, (cmd, ctx) ->
                ctx.success(UserRegistered.builder()
                        .username(cmd.username())
                        .password(cmd.password())
                        .create()));

        behaviourBuilder.setEventHandler(UserRegistered.class, (evt, currentBehaviour) ->
                createdBehaviour(UserState.builder()
                        .username(evt.username())
                        .password(evt.password())
                        .create()));

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
