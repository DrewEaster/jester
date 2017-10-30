package com.dreweaster.ddd.jester.application.repository.deduplicating.monitoring;

import com.dreweaster.ddd.jester.domain.*;

public interface AggregateRepositoryReporter {

    <A extends Aggregate<C, E, State>, C extends DomainCommand, E extends DomainEvent, State> CommandHandlingProbe<A,C,E,State> createProbe(
            AggregateType<A,C,E,State> aggregateType, AggregateId aggregateId);
}
