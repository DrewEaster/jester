package com.dreweaster.ddd.jester.domain;

/**
 */
public class CausationId {

    public static CausationId of(String id) {
        if (id == null) {
            throw new IllegalArgumentException("id cannot be null");
        }
        return new CausationId(id);
    }

    private String id;

    private CausationId(String id) {
        this.id = id;
    }

    public String get() {
        return id;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        CausationId causationId = (CausationId) o;

        if (!id.equals(causationId.id)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        return id.hashCode();
    }

    @Override
    public String toString() {
        return "CausationId{" +
                "id='" + id + '\'' +
                '}';
    }
}
