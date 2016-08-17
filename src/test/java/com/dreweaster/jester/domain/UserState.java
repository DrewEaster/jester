package com.dreweaster.jester.domain;

public class UserState {

    public static UserState EMPTY = of("", "");

    public static UserState of(String username, String password) {
        return new UserState(username, password);
    }

    private String username;

    private String password;

    private int failedLoginAttempts = 0;

    private UserState(String username, String password) {
        this.username = username;
        this.password = password;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public int getFailedLoginAttempts() {
        return failedLoginAttempts;
    }

    public UserState withNewPassword(String newPassword) {
        return UserState.of(username, newPassword);
    }

    public UserState withIncrementedFailedLoginAttempts() {
        UserState newState = UserState.of(username, password);
        newState.failedLoginAttempts = failedLoginAttempts + 1;
        return newState;
    }
}