package com.dreweaster.jester.domain;

public class UsernameChanged extends UserEvent {

    public static UsernameChanged of(String username) {
        return new UsernameChanged(username);
    }

    private String username;

    public UsernameChanged(String username) {
        this.username = username;
    }

    public String getUsername() {
        return username;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        UsernameChanged that = (UsernameChanged) o;

        return !(username != null ? !username.equals(that.username) : that.username != null);
    }

    @Override
    public int hashCode() {
        return username != null ? username.hashCode() : 0;
    }

    @Override
    public String toString() {
        return "UsernameChanged{" +
                "username='" + username + '\'' +
                '}';
    }
}
