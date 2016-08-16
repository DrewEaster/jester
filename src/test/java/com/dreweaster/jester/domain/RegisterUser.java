package com.dreweaster.jester.domain;

/**
 */
public class RegisterUser extends UserCommand {

    public static RegisterUser of(String username, String password) {
        return new RegisterUser(username, password);
    }

    private String username;

    private String password;

    public RegisterUser(String username, String password) {
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
        return "RegisterUser{" +
                "password='" + password + '\'' +
                ", username='" + username + '\'' +
                '}';
    }
}
