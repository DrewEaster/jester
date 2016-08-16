package com.dreweaster.jester.domain;

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
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        UserRegistered that = (UserRegistered) o;

        if (username != null ? !username.equals(that.username) : that.username != null) return false;
        return !(password != null ? !password.equals(that.password) : that.password != null);
    }

    @Override
    public int hashCode() {
        int result = username != null ? username.hashCode() : 0;
        result = 31 * result + (password != null ? password.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "UserRegistered{" +
                "password='" + password + '\'' +
                ", username='" + username + '\'' +
                '}';
    }
}
