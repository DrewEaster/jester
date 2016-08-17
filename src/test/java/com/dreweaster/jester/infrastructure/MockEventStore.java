package com.dreweaster.jester.infrastructure;

import com.dreweaster.jester.application.eventstore.EventStore;
import com.dreweaster.jester.application.eventstore.PersistedEvent;
import com.dreweaster.jester.domain.Aggregate;
import com.dreweaster.jester.domain.AggregateId;
import com.dreweaster.jester.domain.CommandId;
import com.dreweaster.jester.domain.DomainEvent;
import javaslang.concurrent.Future;
import javaslang.concurrent.Promise;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;


public class MockEventStore implements EventStore {

    private Map<Class, List> eventStorage = new HashMap<>();

    private boolean loadErrorState = false;

    private boolean saveErrorState = false;

    public void reset() {
        eventStorage.clear();
    }

    public void toggleLoadErrorStateOn() {
        loadErrorState = true;
    }

    public void toggleLoadErrorStateOff() {
        loadErrorState = false;
    }

    public void toggleSaveErrorStateOn() {
        saveErrorState = true;
    }

    public void toggleSaveErrorStateOff() {
        saveErrorState = false;
    }

    @Override
    public synchronized <A extends Aggregate<?, E, ?>, E extends DomainEvent> Future<List<PersistedEvent<A, E>>> loadEvents(
            Class<A> aggregateType,
            AggregateId aggregateId) {

        if (loadErrorState) {
            Promise<List<PersistedEvent<A, E>>> promise = Promise.make();
            promise.failure(new OptimisticConcurrencyException());
            return Future.failed(new IllegalStateException());
        }
        return Future.successful(persistedEventsFor(aggregateType, aggregateId));
    }

    @SuppressWarnings("unchecked")
    @Override
    public synchronized <A extends Aggregate<?, E, ?>, E extends DomainEvent> Future<List<PersistedEvent<A, E>>> saveEvents(
            Class<A> aggregateType,
            AggregateId aggregateId,
            CommandId commandId,
            List<E> rawEvents,
            Long expectedSequenceNumber) {

        if (saveErrorState) {
            return Future.failed(new IllegalStateException());
        } else {
            // Optimistic concurrency check
            if (aggregateHasBeenModified(aggregateType, aggregateId, expectedSequenceNumber)) {
                return Future.failed(new OptimisticConcurrencyException());
            }

            List<PersistedEvent<A, E>> persistedEvents = new ArrayList<>();

            Long nextSequenceNumber = expectedSequenceNumber + 1;

            for (E rawEvent : rawEvents) {
                persistedEvents.add(new MockPersistedEvent<>(
                        aggregateType,
                        aggregateId,
                        commandId,
                        rawEvent,
                        nextSequenceNumber
                ));

                nextSequenceNumber = nextSequenceNumber + 1;
            }

            List aggregateEvents = eventStorage.get(aggregateType);

            if (aggregateEvents == null) {
                aggregateEvents = new ArrayList<>();
                eventStorage.put(aggregateType, aggregateEvents);
            }

            aggregateEvents.addAll(persistedEvents);

            return Future.successful(persistedEvents);
        }
    }

    @SuppressWarnings("unchecked")
    private <A extends Aggregate<?, E, ?>, E extends DomainEvent> List<PersistedEvent<A, E>> persistedEventsFor(
            Class<A> aggregateType,
            AggregateId aggregateId) {

        if (eventStorage.containsKey(aggregateType)) {
            List<PersistedEvent<A, E>> aggregateEvents = (List<PersistedEvent<A, E>>) eventStorage.get(aggregateType);
            if (aggregateEvents != null) {
                return aggregateEvents.stream().filter(persistedEvent ->
                        persistedEvent.aggregateId().equals(aggregateId)).collect(Collectors.toList());
            }
        }

        return Collections.emptyList();
    }

    private <A extends Aggregate<?, E, ?>, E extends DomainEvent> boolean aggregateHasBeenModified(
            Class<A> aggregateType,
            AggregateId aggregateId,
            Long expectedSequenceNumber) {
        List<PersistedEvent<A, E>> persistedEvents = persistedEventsFor(aggregateType, aggregateId);
        if (persistedEvents.size() > 0) {
            PersistedEvent<A, E> mostRecentEvent = persistedEvents.get(persistedEvents.size() - 1);
            return !mostRecentEvent.sequenceNumber().equals(expectedSequenceNumber);
        }
        return false;
    }

    private class MockPersistedEvent<A extends Aggregate<?, E, ?>, E extends DomainEvent> implements PersistedEvent<A, E> {

        private AggregateId aggregateId;

        private Class<A> aggregateType;

        private CommandId commandId;

        private E rawEvent;

        private LocalDateTime timestamp = LocalDateTime.now();

        private Long sequenceNumber;

        public MockPersistedEvent(
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
        @SuppressWarnings("unchecked")
        public Class<E> eventType() {
            return (Class<E>) rawEvent.getClass();
        }

        @Override
        public E rawEvent() {
            return rawEvent;
        }

        @Override
        public LocalDateTime timestamp() {
            return timestamp;
        }

        @Override
        public Long sequenceNumber() {
            return sequenceNumber;
        }

        @Override
        public String toString() {
            return "MockPersistedEvent{" +
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
