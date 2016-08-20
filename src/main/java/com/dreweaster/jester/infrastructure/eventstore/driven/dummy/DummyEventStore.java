package com.dreweaster.jester.infrastructure.eventstore.driven.dummy;

import com.dreweaster.jester.domain.CommandId;
import com.dreweaster.jester.domain.Aggregate;
import com.dreweaster.jester.domain.AggregateId;
import com.dreweaster.jester.domain.DomainEvent;
import com.dreweaster.jester.application.eventstore.EventStore;
import com.dreweaster.jester.application.eventstore.PersistedEvent;
import com.dreweaster.jester.application.eventstore.StreamEvent;
import javaslang.Tuple2;
import javaslang.collection.HashMap;
import javaslang.collection.List;
import javaslang.collection.Map;
import javaslang.concurrent.Future;

import java.time.LocalDateTime;


public class DummyEventStore implements EventStore {

    private Map<Class, List> eventStorage = HashMap.empty();

    @Override
    public synchronized <A extends Aggregate<?, E, ?>, E extends DomainEvent> Future<List<PersistedEvent<A, E>>> loadEvents(
            Class<A> aggregateType,
            AggregateId aggregateId) {

        return Future.successful(persistedEventsFor(aggregateType, aggregateId));
    }

    @Override
    public <A extends Aggregate<?, E, ?>, E extends DomainEvent> Future<List<PersistedEvent<A, E>>> loadEvents(
            Class<A> aggregateType, AggregateId aggregateId, Long afterSequenceNumber) {

        return Future.successful(persistedEventsFor(aggregateType, aggregateId)
                .filter(event -> event.sequenceNumber() > afterSequenceNumber));
    }

    @SuppressWarnings("unchecked")
    @Override
    public synchronized <A extends Aggregate<?, E, ?>, E extends DomainEvent> Future<List<PersistedEvent<A, E>>> saveEvents(
            Class<A> aggregateType,
            AggregateId aggregateId,
            CommandId commandId,
            List<E> rawEvents,
            Long expectedSequenceNumber) {

        // Optimistic concurrency check
        if (aggregateHasBeenModified(aggregateType, aggregateId, expectedSequenceNumber)) {
            return Future.failed(new OptimisticConcurrencyException());
        }

        List<PersistedEvent<A, E>> persistedEvents =
                rawEvents.foldLeft(new Tuple2<Long, List<PersistedEvent<A, E>>>(expectedSequenceNumber + 1, List.empty()), (acc, e) ->
                        new Tuple2<>(acc._1 + 1, acc._2.append(
                                new DummyPersistedEvent<>(
                                        aggregateType,
                                        aggregateId,
                                        commandId,
                                        e,
                                        acc._1
                                ))))._2;

        eventStorage = eventStorage.put(aggregateType, eventStorage.get(aggregateType)
                .getOrElse(List.empty())
                .appendAll(persistedEvents));

        return Future.successful(persistedEvents);
    }

    /*@Override
    public <A extends Aggregate<?, E, ?>, E extends DomainEvent> Publisher<StreamEvent<A, E>> stream(
            Class<A> aggregateType,
            Optional<Long> fromOffsetOpt) {

        // TODO: Need to implement polling loop
        // TODO: Where should configuration for polling frequency live?
        // TODO: Polling frequency could be something you can specify as a parameter to this method?
        // TODO: Consumers of a stream care most about polling frequency

        List<StreamEvent<A, E>> streamEvents = streamEventsFor(aggregateType);

        Long fromOffset = fromOffsetOpt.orElse(0L);

        if (fromOffset <= streamEvents.size()) {

            List<StreamEvent<A, E>> fromOffsetEvents = streamEvents.subList(
                    fromOffset.intValue(),
                    streamEvents.size());

            // FIXME: This doesn't work - subscribers aren't seeing events
            return RxReactiveStreams.toPublisher(Observable.from(fromOffsetEvents));
        }

        return RxReactiveStreams.toPublisher(Observable.from(Collections.emptyList()));
    }*/

    /*private <A extends Aggregate<?, E, ?>, E extends DomainEvent> List<StreamEvent<A, E>> streamEventsFor(Class<A> aggregateType) {
        List<PersistedEvent<A, E>> persistedEvents = persistedEventsFor(aggregateType);
        List<StreamEvent<A, E>> streamEvents = new ArrayList<>();
        for (long i = 0; i < persistedEvents.size(); i++) {
            streamEvents.add(new DummyStreamEvent<>(persistedEvents.get(Long.valueOf(i).intValue()), i));
        }
        return streamEvents;
    }*/

    @SuppressWarnings("unchecked")
    private <A extends Aggregate<?, E, ?>, E extends DomainEvent> List<PersistedEvent<A, E>> persistedEventsFor(
            Class<A> aggregateType) {
        return eventStorage.get(aggregateType).getOrElse(List.empty());
    }

    @SuppressWarnings("unchecked")
    private <A extends Aggregate<?, E, ?>, E extends DomainEvent> List<PersistedEvent<A, E>> persistedEventsFor(
            Class<A> aggregateType,
            AggregateId aggregateId) {

        return eventStorage.get(aggregateType)
                .map(aggregateEvents -> aggregateEvents
                        .filter(e -> ((PersistedEvent<A, E>) e).aggregateId().equals(aggregateId)))
                .getOrElse(List.empty());
    }

    private <A extends Aggregate<?, E, ?>, E extends DomainEvent> boolean aggregateHasBeenModified(
            Class<A> aggregateType,
            AggregateId aggregateId,
            Long expectedSequenceNumber) {

        return persistedEventsFor(aggregateType, aggregateId)
                .lastOption()
                .map(event -> !event.sequenceNumber().equals(expectedSequenceNumber))
                .getOrElse(false);
    }

    private class DummyStreamEvent<A extends Aggregate<?, E, ?>, E extends DomainEvent> implements StreamEvent<A, E> {

        private PersistedEvent<A, E> persistedEvent;

        private long offset;

        public DummyStreamEvent(PersistedEvent<A, E> persistedEvent, long offset) {
            this.persistedEvent = persistedEvent;
            this.offset = offset;
        }

        @Override
        public Long offset() {
            return offset;
        }

        @Override
        public Class<A> aggregateType() {
            return persistedEvent.aggregateType();
        }

        @Override
        public AggregateId aggregateId() {
            return persistedEvent.aggregateId();
        }

        @Override
        public CommandId commandId() {
            return persistedEvent.commandId();
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
        public LocalDateTime timestamp() {
            return persistedEvent.timestamp();
        }

        @Override
        public Long sequenceNumber() {
            return persistedEvent.sequenceNumber();
        }
    }

    private class DummyPersistedEvent<A extends Aggregate<?, E, ?>, E extends DomainEvent> implements PersistedEvent<A, E> {

        private AggregateId aggregateId;

        private Class<A> aggregateType;

        private CommandId commandId;

        private E rawEvent;

        private LocalDateTime timestamp = LocalDateTime.now();

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
