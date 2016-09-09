package com.dreweaster.ddd.jester.application.eventstore;

import com.dreweaster.ddd.jester.domain.Aggregate;
import com.dreweaster.ddd.jester.domain.AggregateId;
import com.dreweaster.ddd.jester.domain.AggregateType;
import javaslang.concurrent.Future;
import javaslang.control.Option;

public interface SnapshotStore {

    interface Snapshot<State> {

        Long sequenceNumber();

        State get();
    }

    <A extends Aggregate<?, ?, State>, State> Future<Option<Snapshot<State>>> loadSnapshot(
            AggregateType<A, ?, ?, State> aggregateType,
            AggregateId aggregateId);

    <A extends Aggregate<?, ?, State>, State> Future<Snapshot<State>> saveSnapshot(
            AggregateType<A, ?, ?, State> aggregateType,
            AggregateId aggregateId,
            Long sequenceNumber,
            State state);
}
