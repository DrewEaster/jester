package com.dreweaster.jester.domain;

public class FailedLoginAttemptsIncremented extends UserEvent {

    private static final FailedLoginAttemptsIncremented INSTANCE = new FailedLoginAttemptsIncremented();

    public static FailedLoginAttemptsIncremented make() {
        return INSTANCE;
    }
}
