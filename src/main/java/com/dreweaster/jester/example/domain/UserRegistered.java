package com.dreweaster.jester.example.domain;

public class UserRegistered extends UserEvent {

    public static UserRegistered of(String username, String password) {
        return new UserRegistered(username, password);
    }

    private String username;

    private String password;

    public UserRegistered(String username, String password) {
        this.username = username;
        this.password = password;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    @Override
    public String toString() {
        return "UserRegistered{" +
                "password='" + password + '\'' +
                ", username='" + username + '\'' +
                '}';
    }
}
