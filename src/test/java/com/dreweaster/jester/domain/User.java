package com.dreweaster.jester.domain;

// TODO: Deal with snapshots when implemented
public class User extends Aggregate<UserCommand, UserEvent, UserState> {

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

        // Command Handlers
        behaviourBuilder.setCommandHandler(RegisterUser.class, (cmd, ctx) ->
                        ctx.error(new AlreadyRegistered())
        );

        behaviourBuilder.setCommandHandler(ChangePassword.class, (cmd, ctx) ->
                        ctx.success(PasswordChanged.of(
                                cmd.getPassword(),
                                ctx.currentState().getPassword()))
        );

        behaviourBuilder.setCommandHandler(ChangeUsername.class, (cmd, ctx) ->
                        ctx.success(UsernameChanged.of(
                                cmd.getUsername()))
        );

        behaviourBuilder.setCommandHandler(IncrementFailedLoginAttempts.class, (cmd, ctx) -> {
            if (ctx.currentState().getFailedLoginAttempts() < 3) {
                return ctx.success(
                        FailedLoginAttemptsIncremented.make());
            } else {
                return ctx.success(
                        FailedLoginAttemptsIncremented.make(),
                        UserLocked.make());
            }
        });

        // Event Handlers
        behaviourBuilder.setEventHandler(PasswordChanged.class, (evt, currentBehaviour) ->
                        currentBehaviour.withState(currentBehaviour.state().withNewPassword(evt.getNewPassword()))
        );

        behaviourBuilder.setEventHandler(FailedLoginAttemptsIncremented.class, (evt, currentBehaviour) ->
                        currentBehaviour.withState(currentBehaviour.state().withIncrementedFailedLoginAttempts())
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
        BehaviourBuilder<UserCommand, UserEvent, UserState> behaviourBuilder =
                newBehaviourBuilder(state);

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
                        ctx.success(FailedLoginAttemptsIncremented.make())
        );

        // Event Handlers
        behaviourBuilder.setEventHandler(FailedLoginAttemptsIncremented.class, (evt, currentBehaviour) ->
                        currentBehaviour.withState(currentBehaviour.state().withIncrementedFailedLoginAttempts())
        );

        return behaviourBuilder.build();
    }
}
