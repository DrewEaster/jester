package com.dreweaster.ddd.jester.example.domain.aggregates.user;

import com.dreweaster.ddd.jester.domain.Aggregate;
import com.dreweaster.ddd.jester.domain.Behaviour;
import com.dreweaster.ddd.jester.domain.BehaviourBuilder;
import com.dreweaster.ddd.jester.domain.AggregateType;
import com.dreweaster.ddd.jester.example.domain.aggregates.user.commands.UserCommand;
import com.dreweaster.ddd.jester.example.domain.aggregates.user.events.UserEvent;
import com.dreweaster.ddd.jester.example.domain.aggregates.user.events.*;
import com.dreweaster.ddd.jester.example.domain.aggregates.user.commands.*;
import com.dreweaster.ddd.jester.example.domain.aggregates.user.UserState;

// TODO: Deal with snapshots when implemented
public class User extends Aggregate<UserCommand, UserEvent, UserState> {

    // NEVER CHANGE THE TYPE NAME (even if class is renamed)!!!
    public static final AggregateType<User, UserCommand, UserEvent, UserState> TYPE = AggregateType.of("user", User.class);

    private static final UserState EMPTY_STATE = UserState.builder()
            .username("")
            .password("")
            .failedLoginAttempts(0)
            .create();

    public static class AlreadyRegistered extends RuntimeException {
        public AlreadyRegistered() {
            super("User has already been registered!");
        }
    }

    public static class UserIsLocked extends RuntimeException {
        public UserIsLocked() {
            super("The user has been locked!");
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

        BehaviourBuilder<UserCommand, UserEvent, UserState> behaviourBuilder = newBehaviourBuilder(EMPTY_STATE);

        behaviourBuilder.setCommandHandler(RegisterUser.class, (cmd, ctx) ->
                ctx.success(UserRegistered.builder()
                        .username(cmd.username())
                        .password(cmd.password())
                        .create()));

        behaviourBuilder.setEventHandler(UserRegistered.class, (evt, currentBehaviour) ->
                createdBehaviour(currentBehaviour.state()
                        .withUsername(evt.username())
                        .withPassword(evt.password())));

        return behaviourBuilder.build();
    }

    /**
     * This is the post-created behaviour
     *
     * @param state the state of the aggregate
     * @return the post-created behaviour
     */
    public Behaviour<UserCommand, UserEvent, UserState> createdBehaviour(UserState state) {
        BehaviourBuilder<UserCommand, UserEvent, UserState> behaviourBuilder = newBehaviourBuilder(state);

        // Command Handlers
        behaviourBuilder.setCommandHandler(RegisterUser.class, (cmd, ctx) ->
                        ctx.error(new AlreadyRegistered())
        );

        behaviourBuilder.setCommandHandler(ChangePassword.class, (cmd, ctx) ->
                        ctx.success(PasswordChanged.builder()
                                .password(cmd.password())
                                .oldPassword(ctx.currentState().password())
                                .create())
        );

        behaviourBuilder.setCommandHandler(ChangeUsername.class, (cmd, ctx) ->
                        ctx.success(UsernameChanged.builder()
                                .username(cmd.username())
                                .create())
        );

        behaviourBuilder.setCommandHandler(IncrementFailedLoginAttempts.class, (cmd, ctx) -> {
            if (ctx.currentState().failedLoginAttempts() < 3) {
                return ctx.success(FailedLoginAttemptsIncremented.of());
            } else {
                return ctx.success(FailedLoginAttemptsIncremented.of(), UserLocked.of());
            }
        });

        // Event Handlers
        behaviourBuilder.setEventHandler(PasswordChanged.class, (evt, currentBehaviour) ->
                        currentBehaviour.withState(currentBehaviour.state().withPassword(evt.password()))
        );

        behaviourBuilder.setEventHandler(FailedLoginAttemptsIncremented.class, (evt, currentBehaviour) ->
                        currentBehaviour.withState(currentBehaviour.state().withFailedLoginAttempts(
                                currentBehaviour.state().failedLoginAttempts() + 1))
        );

        behaviourBuilder.setEventHandler(UserLocked.class, (evt, currentBehaviour) ->
                        lockedBehaviour(currentBehaviour.state())
        );

        return behaviourBuilder.build();
    }

    /**
     * This is the locked behaviour
     *
     * @param state the state of the aggregate
     * @return the locked behaviour
     */
    public Behaviour<UserCommand, UserEvent, UserState> lockedBehaviour(UserState state) {
        BehaviourBuilder<UserCommand, UserEvent, UserState> behaviourBuilder = newBehaviourBuilder(state);

        // Command Handlers
        behaviourBuilder.setCommandHandler(RegisterUser.class, (cmd, ctx) ->
                        ctx.error(new AlreadyRegistered())
        );

        behaviourBuilder.setCommandHandler(ChangePassword.class, (cmd, ctx) ->
                        ctx.error(new UserIsLocked())
        );

        behaviourBuilder.setCommandHandler(ChangeUsername.class, (cmd, ctx) ->
                        ctx.error(new UserIsLocked())
        );

        behaviourBuilder.setCommandHandler(IncrementFailedLoginAttempts.class, (cmd, ctx) ->
                        ctx.success(FailedLoginAttemptsIncremented.of())
        );

        // Event Handlers
        behaviourBuilder.setEventHandler(FailedLoginAttemptsIncremented.class, (evt, currentBehaviour) ->
                        currentBehaviour.withState(currentBehaviour.state().withFailedLoginAttempts(
                                currentBehaviour.state().failedLoginAttempts() + 1))
        );

        return behaviourBuilder.build();
    }
}
