package com.dreweaster.ddd.framework;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;


public class DummyEventStore implements EventStore {

    public static class OptimisticConcurrencyException extends RuntimeException {

    }

    private Map<Class, Map<AggregateId, List>> eventStorage = new HashMap<>();

    @SuppressWarnings("unchecked")
    @Override
    public synchronized <A extends Aggregate<?, E, ?>, E extends DomainEvent> CompletionStage<List<PersistedEvent<A, E>>> loadEvents(
            Class<A> aggregateType,
            AggregateId aggregateId) {

        if (eventStorage.containsKey(aggregateType)) {
            Map<AggregateId, List> aggregateEvents = eventStorage.get(aggregateType);
            if (aggregateEvents.containsKey(aggregateId)) {
                List events = aggregateEvents.get(aggregateId);
                return CompletableFuture.completedFuture((List<PersistedEvent<A, E>>) events);
            }
        }

        return CompletableFuture.completedFuture(Collections.<PersistedEvent<A, E>>emptyList());
    }

    @SuppressWarnings("unchecked")
    @Override
    public synchronized <A extends Aggregate<?, E, ?>, E extends DomainEvent> CompletionStage<List<PersistedEvent<A, E>>> saveEvents(
            Class<A> aggregateType,
            AggregateId aggregateId,
            CommandId commandId,
            List<E> rawEvents,
            Long expectedSequenceNumber) {

        // Optimistic concurrency check
        if (eventStorage.containsKey(aggregateType)) {
            Map<AggregateId, List> aggregateEvents = eventStorage.get(aggregateType);
            if (aggregateEvents.containsKey(aggregateId)) {
                List<PersistedEvent<A, E>> events = (List<PersistedEvent<A, E>>) aggregateEvents.get(aggregateId);
                if (sequenceNumberConflicts(expectedSequenceNumber, events)) {
                    CompletableFuture<List<PersistedEvent<A, E>>> completableFuture = new CompletableFuture<>();
                    completableFuture.completeExceptionally(new OptimisticConcurrencyException());
                    return completableFuture;
                }
            }
        }

        List<PersistedEvent<A, E>> persistedEvents = new ArrayList<>();

        Long nextSequenceNumber = expectedSequenceNumber + 1;

        for (E rawEvent : rawEvents) {
            persistedEvents.add(new DummyPersistedEvent<>(
                    aggregateType,
                    aggregateId,
                    commandId,
                    rawEvent,
                    nextSequenceNumber
            ));

            nextSequenceNumber = nextSequenceNumber + 1;
        }

        Map<AggregateId, List> aggregateEvents = eventStorage.get(aggregateType);
        // TODO: Complete updating the event store

        return CompletableFuture.completedFuture(persistedEvents);
    }

    private <A extends Aggregate<?, E, ?>, E extends DomainEvent> boolean sequenceNumberConflicts(
            Long expectedSequenceNumber,
            List<PersistedEvent<A, E>> events) {
        return events.stream().filter(evt -> evt.sequenceNumber().equals(expectedSequenceNumber)).findFirst().isPresent();
    }

    private class DummyPersistedEvent<A extends Aggregate<?, E, ?>, E extends DomainEvent> implements PersistedEvent<A, E> {

        private AggregateId aggregateId;

        private Class<A> aggregateType;

        private CommandId commandId;

        private E rawEvent;

        private LocalDate timestamp = LocalDate.now();

        private Long sequenceNumber;

        public DummyPersistedEvent(
                Class<A> aggregateType,
                AggregateId aggregateId,
                CommandId commandId,
                E rawEvent,
                Long sequenceNumber) {
            this.aggregateId = aggregateId;
            this.aggregateType = aggregateType;
            this.commandId = commandId;
            this.rawEvent = rawEvent;
            this.sequenceNumber = sequenceNumber;
        }

        @Override
        public AggregateId aggregateId() {
            return aggregateId;
        }

        @Override
        public Class<A> aggregateType() {
            return aggregateType;
        }

        @Override
        public CommandId commandId() {
            return commandId;
        }

        @Override
        public E rawEvent() {
            return rawEvent;
        }

        @Override
        public LocalDate timestamp() {
            return timestamp;
        }

        @Override
        public Long sequenceNumber() {
            return sequenceNumber;
        }

        @Override
        public String toString() {
            return "DummyPersistedEvent{" +
                    "aggregateId=" + aggregateId +
                    ", aggregateType=" + aggregateType +
                    ", commandId=" + commandId +
                    ", rawEvent=" + rawEvent +
                    ", sequenceNumber=" + sequenceNumber +
                    ", timestamp=" + timestamp +
                    '}';
        }
    }
}
