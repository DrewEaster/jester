package com.dreweaster.jester.domain;

public class PasswordChanged extends UserEvent {

    public static PasswordChanged of(String newPassword, String oldPassword) {
        return new PasswordChanged(newPassword, oldPassword);
    }

    private String newPassword;

    private String oldPassword;

    public PasswordChanged(String newPassword, String oldPassword) {
        this.newPassword = newPassword;
        this.oldPassword = oldPassword;
    }

    public String getNewPassword() {
        return newPassword;
    }

    public String getOldPassword() {
        return oldPassword;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        PasswordChanged that = (PasswordChanged) o;

        if (newPassword != null ? !newPassword.equals(that.newPassword) : that.newPassword != null) return false;
        return !(oldPassword != null ? !oldPassword.equals(that.oldPassword) : that.oldPassword != null);
    }

    @Override
    public int hashCode() {
        int result = newPassword != null ? newPassword.hashCode() : 0;
        result = 31 * result + (oldPassword != null ? oldPassword.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "PasswordChanged{" +
                "newPassword='" + newPassword + '\'' +
                ", oldPassword='" + oldPassword + '\'' +
                '}';
    }
}
