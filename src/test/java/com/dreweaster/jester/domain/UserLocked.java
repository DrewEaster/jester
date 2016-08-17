package com.dreweaster.jester.domain;

public class UserLocked extends UserEvent {

    private static final UserLocked INSTANCE = new UserLocked();

    public static UserLocked make() {
        return INSTANCE;
    }
}
