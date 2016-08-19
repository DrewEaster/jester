package com.dreweaster.jester.application.eventstore;

import com.dreweaster.jester.domain.Aggregate;
import com.dreweaster.jester.domain.AggregateId;
import com.dreweaster.jester.domain.CommandId;
import com.dreweaster.jester.domain.DomainEvent;
import javaslang.concurrent.Future;
import javaslang.control.Option;

import java.util.List;

public interface SnapshotStore {

    interface Snapshot<State> {

        Long sequenceNumber();

        State get();
    }

    <A extends Aggregate<?, ?, State>, State> Future<Option<Snapshot<State>>> loadSnapshot(
            Class<A> aggregateType,
            AggregateId aggregateId);

    <A extends Aggregate<?, ?, State>, State> Future<Snapshot<State>> saveSnapshot(
            Class<A> aggregateType,
            AggregateId aggregateId,
            Long sequenceNumber,
            State state);
}
