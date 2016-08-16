package com.dreweaster.jester.domain;

/**
 */
public class ChangePassword extends UserCommand {

    public static ChangePassword of(String password) {
        return new ChangePassword(password);
    }

    private String password;

    public ChangePassword(String password) {
        this.password = password;
    }

    public String getPassword() {
        return password;
    }

    @Override
    public String toString() {
        return "ChangePassword{" +
                "password='" + password + '\'' +
                '}';
    }
}
