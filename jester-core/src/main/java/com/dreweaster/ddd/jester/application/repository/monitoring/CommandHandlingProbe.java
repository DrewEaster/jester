package com.dreweaster.ddd.jester.application.repository.monitoring;

import com.dreweaster.ddd.jester.application.eventstore.PersistedEvent;
import com.dreweaster.ddd.jester.domain.Aggregate;
import com.dreweaster.ddd.jester.domain.AggregateRepository.CommandEnvelope;
import com.dreweaster.ddd.jester.domain.AggregateRepository.CommandHandlingResult;
import com.dreweaster.ddd.jester.domain.DomainCommand;
import com.dreweaster.ddd.jester.domain.DomainEvent;
import io.vavr.collection.List;

public interface CommandHandlingProbe<A extends Aggregate<C, E, State>, C extends DomainCommand, E extends DomainEvent, State> {

    void startedHandling(CommandEnvelope<C> command);

    void startedLoadingEvents();

    void finishedLoadingEvents(List<PersistedEvent<A,E>> previousEvents);

    void finishedLoadingEvents(Throwable unexpectedException);

    void startedApplyingCommand();

    void commandApplicationAccepted(List<? super E> events, boolean deduplicated);

    void commandApplicationRejected(Throwable rejection, boolean deduplicated);

    void commandApplicationFailed(Throwable unexpectedException);

    void startedPersistingEvents(List<? super E> events, long expectedSequenceNumber);

    void startedPersistingEvents(List<? super E> events, State state, long expectedSequenceNumber);

    void finishedPersistingEvents(List<PersistedEvent<A,E>> persistedEvents);

    void finishedPersistingEvents(Throwable unexpectedException);

    void finishedHandling(CommandHandlingResult<C,E> result);

    void finishedHandling(Throwable unexpectedException);
}
