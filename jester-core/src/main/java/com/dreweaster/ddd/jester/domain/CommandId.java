package com.dreweaster.ddd.jester.domain;

/**
 */
public class CommandId {

    public static CommandId of(String id) {
        if (id == null) {
            throw new IllegalArgumentException("id cannot be null");
        }
        return new CommandId(id);
    }

    private String id;

    private CommandId(String id) {
        this.id = id;
    }

    public String get() {
        return id;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        CommandId commandId = (CommandId) o;

        if (!id.equals(commandId.id)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public String toString() {
        return "CommandId{" +
                "id='" + id + '\'' +
                '}';
    }
}
