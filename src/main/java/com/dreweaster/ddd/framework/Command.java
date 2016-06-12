package com.dreweaster.ddd.framework;

import java.util.UUID;

public class Command<T> {

    public static <T> Command<T> of(AggregateId aggregateId, CommandId id, T payload) {
        return new Command<T>(aggregateId, id, payload) {
            @Override
            public AggregateId aggregateId() {
                return super.aggregateId();
            }

            @Override
            public CommandId id() {
                return super.id();
            }
        };
    }

    public static <T> Command<T> of(AggregateId aggregateId, T payload) {
        return new Command<T>(aggregateId, CommandId.of(UUID.randomUUID().toString()), payload) {
            @Override
            public AggregateId aggregateId() {
                return super.aggregateId();
            }

            @Override
            public CommandId id() {
                return super.id();
            }
        };
    }

    private AggregateId aggregateId;

    private CommandId id;

    private T payload;

    public Command(AggregateId aggregateId, CommandId id, T payload) {
        this.id = id;
        this.aggregateId = aggregateId;
        this.payload = payload;
    }

    public AggregateId aggregateId() {
        return aggregateId;
    }

    public CommandId id() {
        return id;
    }

    public T payload() {
        return payload;
    }
}
