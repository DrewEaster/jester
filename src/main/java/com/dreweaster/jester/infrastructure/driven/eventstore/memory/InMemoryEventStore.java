package com.dreweaster.jester.infrastructure.driven.eventstore.memory;

import com.dreweaster.jester.domain.*;
import com.dreweaster.jester.application.eventstore.EventStore;
import com.dreweaster.jester.application.eventstore.PersistedEvent;
import com.dreweaster.jester.application.eventstore.StreamEvent;
import javaslang.Tuple2;
import javaslang.collection.HashMap;
import javaslang.collection.List;
import javaslang.collection.Map;
import javaslang.concurrent.Future;
import javaslang.control.Option;

import java.time.LocalDateTime;

// TODO: Delegate serialisation to an EventPayloadMapper
public class InMemoryEventStore implements EventStore {

    private Map<AggregateType, List> eventStorage = HashMap.empty();

    public void clear() {
        eventStorage = HashMap.empty();
    }

    @Override
    public synchronized <A extends Aggregate<?, E, ?>, E extends DomainEvent> Future<List<PersistedEvent<A, E>>> loadEvents(
            AggregateType<A, ?, E, ?> aggregateType,
            AggregateId aggregateId) {

        return Future.successful(persistedEventsFor(aggregateType, aggregateId));
    }

    @Override
    public <A extends Aggregate<?, E, ?>, E extends DomainEvent> Future<List<PersistedEvent<A, E>>> loadEvents(
            AggregateType<A, ?, E, ?> aggregateType, AggregateId aggregateId, Long afterSequenceNumber) {

        return Future.successful(persistedEventsFor(aggregateType, aggregateId)
                .filter(event -> event.sequenceNumber() > afterSequenceNumber));
    }

    @Override
    public <A extends Aggregate<?, E, ?>, E extends DomainEvent> Future<List<StreamEvent<A, E>>> loadEventStream(
            AggregateType<A, ?, E, ?> aggregateType,
            Integer batchSize) {
        List<PersistedEvent<A, E>> persistedEvents = persistedEventsFor(aggregateType);
        return Future.successful(persistedEvents
                .map(event -> streamEventOf(event, persistedEvents.indexOf(event)))
                .take(batchSize));
    }

    @Override
    public <A extends Aggregate<?, E, ?>, E extends DomainEvent> Future<List<StreamEvent<A, E>>> loadEventStream(
            AggregateType<A, ?, E, ?> aggregateType,
            Long afterOffset,
            Integer batchSize) {
        List<PersistedEvent<A, E>> persistedEvents = persistedEventsFor(aggregateType);
        return Future.successful(persistedEvents
                .map(event -> streamEventOf(event, persistedEvents.indexOf(event)))
                .filter(event -> event.offset() > afterOffset)
                .take(batchSize));
    }

    @Override
    public synchronized <A extends Aggregate<?, E, ?>, E extends DomainEvent> Future<List<PersistedEvent<A, E>>> saveEvents(
            AggregateType<A, ?, E, ?> aggregateType,
            AggregateId aggregateId,
            CausationId causationId,
            List<E> rawEvents,
            Long expectedSequenceNumber) {
        return doSaveEvents(aggregateType, aggregateId, causationId, Option.none(), rawEvents, expectedSequenceNumber);
    }

    @Override
    public synchronized <A extends Aggregate<?, E, ?>, E extends DomainEvent> Future<List<PersistedEvent<A, E>>> saveEvents(
            AggregateType<A, ?, E, ?> aggregateType,
            AggregateId aggregateId,
            CausationId causationId,
            CorrelationId correlationId,
            List<E> rawEvents,
            Long expectedSequenceNumber) {
        return doSaveEvents(aggregateType, aggregateId, causationId, Option.of(correlationId), rawEvents, expectedSequenceNumber);
    }

    @SuppressWarnings("unchecked")
    private <A extends Aggregate<?, E, ?>, E extends DomainEvent> Future<List<PersistedEvent<A, E>>> doSaveEvents(
            AggregateType<A, ?, E, ?> aggregateType,
            AggregateId aggregateId,
            CausationId causationId,
            Option<CorrelationId> correlationId,
            List<E> rawEvents,
            Long expectedSequenceNumber) {

        // Optimistic concurrency check
        if (aggregateHasBeenModified(aggregateType, aggregateId, expectedSequenceNumber)) {
            return Future.failed(new OptimisticConcurrencyException());
        }

        List<PersistedEvent<A, E>> persistedEvents =
                rawEvents.foldLeft(new Tuple2<Long, List<PersistedEvent<A, E>>>(expectedSequenceNumber + 1, List.empty()), (acc, e) ->
                        new Tuple2<>(acc._1 + 1, acc._2.append(
                                new SimplePersistedEvent<>(
                                        aggregateType,
                                        aggregateId,
                                        causationId,
                                        correlationId,
                                        e,
                                        acc._1
                                ))))._2;

        eventStorage = eventStorage.put(aggregateType, eventStorage.get(aggregateType)
                .getOrElse(List.empty())
                .appendAll(persistedEvents));

        return Future.successful(persistedEvents);
    }

    @SuppressWarnings("unchecked")
    private <A extends Aggregate<?, E, ?>, E extends DomainEvent> List<PersistedEvent<A, E>> persistedEventsFor(
            AggregateType<A, ?, E, ?> aggregateType) {
        return eventStorage.get(aggregateType).getOrElse(List.empty());
    }

    @SuppressWarnings("unchecked")
    private <A extends Aggregate<?, E, ?>, E extends DomainEvent> List<PersistedEvent<A, E>> persistedEventsFor(
            AggregateType<A, ?, E, ?> aggregateType,
            AggregateId aggregateId) {

        return eventStorage.get(aggregateType)
                .map(aggregateEvents -> aggregateEvents
                        .filter(e -> ((PersistedEvent<A, E>) e).aggregateId().equals(aggregateId)))
                .getOrElse(List.empty());
    }

    private <A extends Aggregate<?, E, ?>, E extends DomainEvent> boolean aggregateHasBeenModified(
            AggregateType<A, ?, E, ?> aggregateType,
            AggregateId aggregateId,
            Long expectedSequenceNumber) {

        return persistedEventsFor(aggregateType, aggregateId)
                .lastOption()
                .map(event -> !event.sequenceNumber().equals(expectedSequenceNumber))
                .getOrElse(false);
    }

    private <A extends Aggregate<?, E, ?>, E extends DomainEvent> StreamEvent<A, E> streamEventOf(
            PersistedEvent<A, E> persistedEvent, long offset) {
        return new SimpleStreamEvent<>(persistedEvent, offset);
    }

    private class SimpleStreamEvent<A extends Aggregate<?, E, ?>, E extends DomainEvent> implements StreamEvent<A, E> {

        private PersistedEvent<A, E> persistedEvent;

        private long offset;

        public SimpleStreamEvent(PersistedEvent<A, E> persistedEvent, long offset) {
            this.persistedEvent = persistedEvent;
            this.offset = offset;
        }

        @Override
        public EventId id() {
            return persistedEvent.id();
        }

        @Override
        public Long offset() {
            return offset;
        }

        @Override
        public AggregateType<A, ?, E, ?> aggregateType() {
            return persistedEvent.aggregateType();
        }

        @Override
        public AggregateId aggregateId() {
            return persistedEvent.aggregateId();
        }

        @Override
        public CausationId causationId() {
            return persistedEvent.causationId();
        }

        @Override
        public Option<CorrelationId> correlationId() {
            return persistedEvent.correlationId();
        }

        @Override
        public E rawEvent() {
            return persistedEvent.rawEvent();
        }

        @Override
        public Class<E> eventType() {
            return persistedEvent.eventType();
        }

        @Override
        public Integer eventVersion() {
            return 1; // TODO: Implement EventPayloadMapper integration
        }

        @Override
        public LocalDateTime timestamp() {
            return persistedEvent.timestamp();
        }

        @Override
        public Long sequenceNumber() {
            return persistedEvent.sequenceNumber();
        }
    }

    private class SimplePersistedEvent<A extends Aggregate<?, E, ?>, E extends DomainEvent> implements PersistedEvent<A, E> {

        private EventId eventId = EventId.createUnique();

        private AggregateId aggregateId;

        private AggregateType<A, ?, E, ?> aggregateType;

        private CausationId causationId;

        private Option<CorrelationId> correlationId;

        private E rawEvent;

        private LocalDateTime timestamp = LocalDateTime.now();

        private Long sequenceNumber;

        public SimplePersistedEvent(
                AggregateType<A, ?, E, ?> aggregateType,
                AggregateId aggregateId,
                CausationId causationId,
                Option<CorrelationId> correlationId,
                E rawEvent,
                Long sequenceNumber) {
            this.aggregateId = aggregateId;
            this.aggregateType = aggregateType;
            this.causationId = causationId;
            this.correlationId = correlationId;
            this.rawEvent = rawEvent;
            this.sequenceNumber = sequenceNumber;
        }

        @Override
        public EventId id() {
            return eventId;
        }

        @Override
        public AggregateId aggregateId() {
            return aggregateId;
        }

        @Override
        public AggregateType<A, ?, E, ?> aggregateType() {
            return aggregateType;
        }

        @Override
        public CausationId causationId() {
            return causationId;
        }

        @Override
        public Option<CorrelationId> correlationId() {
            return correlationId;
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
        public Integer eventVersion() {
            return 1; // TODO: Implement EventPayloadMapper integration
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
            return "SimplePersistedEvent{" +
                    "eventId=" + eventId +
                    ", aggregateId=" + aggregateId +
                    ", aggregateType=" + aggregateType +
                    ", causationId=" + causationId +
                    ", correlationId=" + correlationId +
                    ", rawEvent=" + rawEvent +
                    ", timestamp=" + timestamp +
                    ", sequenceNumber=" + sequenceNumber +
                    '}';
        }
    }
}
