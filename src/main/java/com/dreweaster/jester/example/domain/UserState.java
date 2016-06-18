package com.dreweaster.jester.example.domain;

public class UserState {

    public static UserState EMPTY = of("", "");

    public static UserState of(String username, String password) {
        return new UserState(username, password);
    }

    private String username;

    private String password;

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
}