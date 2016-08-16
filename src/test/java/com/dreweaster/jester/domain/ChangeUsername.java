package com.dreweaster.jester.domain;

/**
 */
public class ChangeUsername extends UserCommand {

    public static ChangeUsername of(String username) {
        return new ChangeUsername(username);
    }

    private String username;

    public ChangeUsername(String username) {
        this.username = username;
    }

    public String getUsername() {
        return username;
    }

    @Override
    public String toString() {
        return "ChangeUsername{" +
                "username='" + username + '\'' +
                '}';
    }
}
