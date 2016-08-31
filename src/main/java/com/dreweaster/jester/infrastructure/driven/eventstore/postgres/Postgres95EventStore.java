package com.dreweaster.jester.infrastructure.driven.eventstore.postgres;

import com.dreweaster.jester.application.eventstore.EventStore;
import com.dreweaster.jester.application.eventstore.PersistedEvent;
import com.dreweaster.jester.application.eventstore.StreamEvent;
import com.dreweaster.jester.domain.*;
import com.dreweaster.jester.application.eventstore.EventPayloadSerialiser;
import javaslang.Tuple2;
import javaslang.collection.List;
import javaslang.concurrent.Future;
import javaslang.control.Option;

import javax.sql.DataSource;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;

/**
 * TODO: Refactor into separate child maven module
 * TODO: Aggregate types and event types - relying on straight class names is not ideal. Need immunity from refactoring
 * TODO: Need to think about event format evolution over time
 * TODO: Implement integration tests using Postgres started by Docker (via Maven).
 */
public class Postgres95EventStore implements EventStore {

    private DataSource dataSource;

    private ExecutorService executorService;

    private EventPayloadSerialiser serialiser;

    public Postgres95EventStore(DataSource dataSource, ExecutorService executorService, EventPayloadSerialiser serialiser) {
        this.dataSource = dataSource;
        this.executorService = executorService;
        this.serialiser = serialiser;
    }

    @Override
    public <A extends Aggregate<?, E, ?>, E extends DomainEvent> Future<List<PersistedEvent<A, E>>> loadEvents(
            AggregateType<A, ?, E, ?> aggregateType, AggregateId aggregateId) {
        return Future.of(executorService, () -> loadEventsForAggregateInstance(aggregateType, aggregateId, Option.none()));
    }

    @Override
    public <A extends Aggregate<?, E, ?>, E extends DomainEvent> Future<List<PersistedEvent<A, E>>> loadEvents(
            AggregateType<A, ?, E, ?> aggregateType, AggregateId aggregateId, Long afterSequenceNumber) {
        return Future.of(executorService, () -> loadEventsForAggregateInstance(
                aggregateType,
                aggregateId,
                Option.of(afterSequenceNumber)));
    }

    @Override
    public <A extends Aggregate<?, E, ?>, E extends DomainEvent> Future<List<StreamEvent<A, E>>> loadEventStream(
            AggregateType<A, ?, E, ?> aggregateType,
            Integer batchSize) {

        return Future.of(executorService, () -> loadEventsForAggregateType(
                aggregateType,
                Option.none(),
                batchSize));
    }

    @Override
    public <A extends Aggregate<?, E, ?>, E extends DomainEvent> Future<List<StreamEvent<A, E>>> loadEventStream(
            AggregateType<A, ?, E, ?> aggregateType,
            Long afterOffset,
            Integer batchSize) {

        return Future.of(executorService, () -> loadEventsForAggregateType(
                aggregateType,
                Option.of(afterOffset),
                batchSize));
    }

    @Override
    public <A extends Aggregate<?, E, ?>, E extends DomainEvent> Future<List<PersistedEvent<A, E>>> saveEvents(
            AggregateType<A, ?, E, ?> aggregateType,
            AggregateId aggregateId,
            CommandId commandId,
            List<E> rawEvents,
            Long expectedSequenceNumber) {

        return Future.of(executorService, () -> saveEventsForAggregateInstance(
                aggregateType,
                aggregateId,
                commandId,
                rawEvents,
                expectedSequenceNumber));
    }

    private <A extends Aggregate<?, E, ?>, E extends DomainEvent> List<PersistedEvent<A, E>> saveEventsForAggregateInstance(
            AggregateType<A, ?, E, ?> aggregateType,
            AggregateId aggregateId,
            CommandId commandId,
            List<E> rawEvents,
            Long expectedSequenceNumber) throws SQLException {

        // TODO: What would sequence number be if no previous events had been saved?

        LocalDateTime timestamp = LocalDateTime.now(); // TODO: Inject clock

        List<PersistedEvent<A, E>> persistedEvents = rawEvents.foldLeft(
                new Tuple2<Long, List<PersistedEvent<A, E>>>(expectedSequenceNumber + 1, List.empty()), (acc, event) -> {

                    Tuple2<String, Integer> serialisedEvent = serialiser.serialise(event);

                    return new Tuple2<>(acc._1 + 1, acc._2.append(
                            new PostgresEvent<>(
                                    aggregateId,
                                    aggregateType,
                                    commandId,
                                    event,
                                    serialisedEvent._1,
                                    serialisedEvent._2,
                                    timestamp,
                                    acc._1)));
                })._2;

        Long latestSequenceNumber = persistedEvents.last().sequenceNumber();

        try (Connection con = dataSource.getConnection();
             PreparedStatement seps = createSaveEventsBatchedPreparedStatement(con, aggregateType, aggregateId, commandId, persistedEvents);
             PreparedStatement saps = createSaveAggregatePreparedStatement(con, aggregateType, aggregateId, latestSequenceNumber, expectedSequenceNumber)) {

            con.setAutoCommit(false);

            try {
                seps.executeBatch();
                int rowsAffected = saps.executeUpdate();
                if (rowsAffected == 1) {
                    con.commit();
                } else {
                    con.rollback();
                    throw new OptimisticConcurrencyException();
                }
            } catch (SQLException ex) {
                con.rollback();
                throw ex;
            }
        }

        return persistedEvents;
    }

    @SuppressWarnings("unchecked")
    private <A extends Aggregate<?, E, ?>, E extends DomainEvent> List<PersistedEvent<A, E>> loadEventsForAggregateInstance(
            AggregateType<A, ?, E, ?> aggregateType,
            AggregateId aggregateId,
            Option<Long> afterSequenceNumber) throws SQLException, ClassNotFoundException {

        try (Connection con = dataSource.getConnection();
             PreparedStatement ps = createEventsForAggregateInstancePreparedStatement(con, aggregateType, aggregateId, afterSequenceNumber);
             ResultSet rs = ps.executeQuery()) {

            ArrayList<PersistedEvent<A, E>> persistedEvents = new ArrayList<>();

            while (rs.next()) {
                persistedEvents.add(resultSetToPersistedEvent(rs, aggregateType));
            }

            return List.ofAll(persistedEvents);
        }
    }

    @SuppressWarnings("unchecked")
    private <A extends Aggregate<?, E, ?>, E extends DomainEvent> List<StreamEvent<A, E>> loadEventsForAggregateType(
            AggregateType<A, ?, E, ?> aggregateType,
            Option<Long> afterOffset,
            Integer batchSize) throws SQLException, ClassNotFoundException {

        try (Connection con = dataSource.getConnection();
             PreparedStatement ps = createEventsForAggregateTypePreparedStatement(con, aggregateType, afterOffset, batchSize);
             ResultSet rs = ps.executeQuery()) {

            ArrayList<StreamEvent<A, E>> persistedEvents = new ArrayList<>();

            while (rs.next()) {
                persistedEvents.add(resultSetToPersistedEvent(rs, aggregateType));
            }

            return List.ofAll(persistedEvents);
        }
    }

    private <A extends Aggregate<?, E, ?>, E extends DomainEvent> PreparedStatement createSaveEventsBatchedPreparedStatement(
            Connection connection,
            AggregateType<A, ?, E, ?> aggregateType,
            AggregateId aggregateId,
            CommandId commandId,
            List<PersistedEvent<A, E>> events) throws SQLException {

        PreparedStatement statement = connection.prepareStatement("" +
                "INSERT INTO domain_event(aggregate_id, aggregate_type, command_id, event_type, event_version, event_payload, event_timestamp, sequence_number) " +
                "VALUES(?,?,?,?,?,?,?,?)");

        for (PersistedEvent<A, E> event : events) {
            statement.setString(1, aggregateId.get());
            statement.setString(2, aggregateType.name());
            statement.setString(3, commandId.get());
            statement.setString(4, event.rawEvent().getClass().getName());
            statement.setInt(5, event.eventVersion());
            statement.setString(6, ((PostgresEvent<A, E>) event).serialisedEvent()); // TODO: Suspicious casting :-)
            statement.setTimestamp(7, Timestamp.valueOf(event.timestamp()));
            statement.setLong(8, event.sequenceNumber());
            statement.addBatch();
        }

        return statement;
    }

    private <A extends Aggregate<?, E, ?>, E extends DomainEvent> PreparedStatement createSaveAggregatePreparedStatement(
            Connection connection,
            AggregateType<A, ?, E, ?> aggregateType,
            AggregateId aggregateId,
            Long newVersion,
            Long expectedPreviousVersion) throws SQLException {

        // TODO: Need to test behaviour and expectations for case when there are no previous events. What should caller send as expectedPreviousVersion?

        PreparedStatement statement = connection.prepareStatement("" +
                "INSERT INTO aggregate_root (aggregate_id,aggregate_type,aggregate_version) " +
                "VALUES (?,?,?) " +
                "ON CONFLICT ON CONSTRAINT aggregate_root_pkey " +
                "DO UPDATE SET aggregate_version = ? WHERE aggregate_root.aggregate_version = ?");

        statement.setString(1, aggregateId.get());
        statement.setString(2, aggregateType.name());
        statement.setLong(3, newVersion);
        statement.setLong(4, newVersion);
        statement.setLong(5, expectedPreviousVersion);

        return statement;
    }

    private <A extends Aggregate<?, E, ?>, E extends DomainEvent> PreparedStatement createEventsForAggregateInstancePreparedStatement(
            Connection connection,
            AggregateType<A, ?, E, ?> aggregateType,
            AggregateId aggregateId,
            Option<Long> afterSequenceNumber) throws SQLException {

        PreparedStatement statement = connection.prepareStatement("" +
                "SELECT global_offset, aggregate_id, aggregate_type, command_id, event_type, event_version, event_payload, event_timestamp, sequence_number " +
                "FROM domain_event " +
                "WHERE aggregate_id = ? AND aggregate_type = ? AND sequence_number > ? " +
                "ORDER BY sequence_number");

        statement.setString(1, aggregateId.get());
        statement.setString(2, aggregateType.name());
        statement.setLong(3, afterSequenceNumber.getOrElse(-1L));

        return statement;
    }

    private <A extends Aggregate<?, E, ?>, E extends DomainEvent> PreparedStatement createEventsForAggregateTypePreparedStatement(
            Connection connection,
            AggregateType<A, ?, E, ?> aggregateType,
            Option<Long> afterOffset,
            Integer batchSize) throws SQLException {

        PreparedStatement statement = connection.prepareStatement("" +
                "SELECT global_offset, aggregate_id, aggregate_type, command_id, event_type, event_version, event_payload, event_timestamp, sequence_number " +
                "FROM domain_event " +
                "WHERE aggregate_type = ? AND global_offset > ? " +
                "ORDER BY global_offset " +
                "LIMIT ?");

        statement.setString(1, aggregateType.name());
        statement.setLong(2, afterOffset.getOrElse(-1L));
        statement.setInt(3, batchSize);

        return statement;
    }

    @SuppressWarnings("unchecked")
    private <A extends Aggregate<?, E, ?>, E extends DomainEvent> StreamEvent<A, E> resultSetToPersistedEvent(
            ResultSet rs,
            AggregateType<A, ?, E, ?> aggregateType)
            throws SQLException, ClassNotFoundException {
        Long offset = rs.getLong(1);
        AggregateId aggregateId = AggregateId.of(rs.getString(2));
        CommandId commandId = CommandId.of(rs.getString(4));
        String eventType = rs.getString(5);
        Integer eventVersion = rs.getInt(6);
        String serialisedEvent = rs.getString(7);
        E rawEvent = serialiser.deserialise(serialisedEvent, eventType, eventVersion);
        LocalDateTime timestamp = rs.getTimestamp(8).toLocalDateTime();
        Long sequenceNumber = rs.getLong(9);
        return new PostgresStreamEvent<>(
                offset,
                aggregateId,
                aggregateType,
                commandId,
                rawEvent,
                serialisedEvent,
                eventVersion,
                timestamp,
                sequenceNumber);
    }

    private class PostgresStreamEvent<A extends Aggregate<?, E, ?>, E extends DomainEvent> extends PostgresEvent<A, E> implements StreamEvent<A, E> {

        private Long offset;

        public PostgresStreamEvent(
                Long offset,
                AggregateId aggregateId,
                AggregateType<A, ?, E, ?> aggregateType,
                CommandId commandId,
                E rawEvent,
                String serialisedEvent,
                Integer eventVersion,
                LocalDateTime timestamp,
                Long sequenceNumber) {
            super(aggregateId, aggregateType, commandId, rawEvent, serialisedEvent, eventVersion, timestamp, sequenceNumber);
            this.offset = offset;
        }

        @Override
        public Long offset() {
            return offset;
        }
    }

    private class PostgresEvent<A extends Aggregate<?, E, ?>, E extends DomainEvent>
            implements PersistedEvent<A, E> {

        private AggregateId aggregateId;

        private AggregateType<A, ?, E, ?> aggregateType;

        private CommandId commandId;

        private E rawEvent;

        private String serialisedEvent;

        private Integer eventVersion;

        private LocalDateTime timestamp = LocalDateTime.now();

        private Long sequenceNumber;

        public PostgresEvent(
                AggregateId aggregateId,
                AggregateType<A, ?, E, ?> aggregateType,
                CommandId commandId,
                E rawEvent,
                String serialisedEvent,
                Integer eventVersion,
                LocalDateTime timestamp,
                Long sequenceNumber) {

            this.aggregateId = aggregateId;
            this.aggregateType = aggregateType;
            this.commandId = commandId;
            this.rawEvent = rawEvent;
            this.serialisedEvent = serialisedEvent;
            this.eventVersion = eventVersion;
            this.timestamp = timestamp;
            this.sequenceNumber = sequenceNumber;
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

        public String serialisedEvent() {
            return serialisedEvent;
        }

        @Override
        public Integer eventVersion() {
            return eventVersion;
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
            return "PostgresEvent{" +
                    ", aggregateId=" + aggregateId +
                    ", aggregateType=" + aggregateType +
                    ", commandId=" + commandId +
                    ", rawEvent=" + rawEvent +
                    ", eventVersion=" + eventVersion +
                    ", timestamp=" + timestamp +
                    ", sequenceNumber=" + sequenceNumber +
                    '}';
        }
    }
}
