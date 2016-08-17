package com.dreweaster.jester.domain;

/**
 */
public class IncrementFailedLoginAttempts extends UserCommand {

    private static final IncrementFailedLoginAttempts INSTANCE = new IncrementFailedLoginAttempts();

    public static IncrementFailedLoginAttempts make() {
        return INSTANCE;
    }
}
