package com.dreweaster.jester.infrastructure.eventstore.driven.postgres;

import com.dreweaster.jester.application.eventstore.EventStore;
import com.dreweaster.jester.application.eventstore.PersistedEvent;
import com.dreweaster.jester.application.eventstore.StreamEvent;
import com.dreweaster.jester.domain.Aggregate;
import com.dreweaster.jester.domain.AggregateId;
import com.dreweaster.jester.domain.CommandId;
import com.dreweaster.jester.domain.DomainEvent;
import com.dreweaster.jester.infrastructure.eventstore.driven.EventPayloadMapper;
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
 */
public class Postgres95EventStore implements EventStore {

    private DataSource dataSource;

    private ExecutorService executorService;

    private EventPayloadMapper mapper;

    public Postgres95EventStore(DataSource dataSource, ExecutorService executorService, EventPayloadMapper mapper) {
        this.dataSource = dataSource;
        this.executorService = executorService;
        this.mapper = mapper;
    }

    @Override
    public <A extends Aggregate<?, E, ?>, E extends DomainEvent> Future<List<PersistedEvent<A, E>>> loadEvents(
            Class<A> aggregateType, AggregateId aggregateId) {
        return Future.of(executorService, () -> loadEventsForAggregateInstance(aggregateType, aggregateId, Option.none()));
    }

    @Override
    public <A extends Aggregate<?, E, ?>, E extends DomainEvent> Future<List<PersistedEvent<A, E>>> loadEvents(
            Class<A> aggregateType, AggregateId aggregateId, Long afterSequenceNumber) {
        return Future.of(executorService, () -> loadEventsForAggregateInstance(
                aggregateType,
                aggregateId,
                Option.of(afterSequenceNumber)));
    }

    @Override
    public <A extends Aggregate<?, E, ?>, E extends DomainEvent> Future<List<StreamEvent<A, E>>> loadEventStream(
            Class<A> aggregateType,
            Integer batchSize) {

        return Future.of(executorService, () -> loadEventsForAggregateType(
                aggregateType,
                Option.none(),
                batchSize));
    }

    @Override
    public <A extends Aggregate<?, E, ?>, E extends DomainEvent> Future<List<StreamEvent<A, E>>> loadEventStream(
            Class<A> aggregateType,
            Long afterOffset,
            Integer batchSize) {

        return Future.of(executorService, () -> loadEventsForAggregateType(
                aggregateType,
                Option.of(afterOffset),
                batchSize));
    }

    @Override
    public <A extends Aggregate<?, E, ?>, E extends DomainEvent> Future<List<PersistedEvent<A, E>>> saveEvents(
            Class<A> aggregateType,
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
            Class<A> aggregateType,
            AggregateId aggregateId,
            CommandId commandId,
            List<E> rawEvents,
            Long expectedSequenceNumber) throws SQLException {

        // TODO: What would sequence number be if no previous events had been saved?

        LocalDateTime timestamp = LocalDateTime.now(); // TODO: Inject clock

        List<PersistedEvent<A, E>> persistedEvents = rawEvents.foldLeft(new Tuple2<Long, List<PersistedEvent<A, E>>>(
                expectedSequenceNumber + 1, List.empty()), (acc, event) -> new Tuple2<>(acc._1 + 1, acc._2.append(
                new PostgresEvent<>(aggregateId, aggregateType, commandId, event, timestamp, acc._1))))._2;

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
            Class<A> aggregateType,
            AggregateId aggregateId,
            Option<Long> afterSequenceNumber) throws SQLException, ClassNotFoundException {

        try (Connection con = dataSource.getConnection();
             PreparedStatement ps = createEventsForAggregateInstancePreparedStatement(con, aggregateType, aggregateId, afterSequenceNumber);
             ResultSet rs = ps.executeQuery()) {

            ArrayList<PersistedEvent<A, E>> persistedEvents = new ArrayList<>();

            while (rs.next()) {
                persistedEvents.add(resultSetToPersistedEvent(rs));
            }

            return List.ofAll(persistedEvents);
        }
    }

    @SuppressWarnings("unchecked")
    private <A extends Aggregate<?, E, ?>, E extends DomainEvent> List<StreamEvent<A, E>> loadEventsForAggregateType(
            Class<A> aggregateType,
            Option<Long> afterOffset,
            Integer batchSize) throws SQLException, ClassNotFoundException {

        try (Connection con = dataSource.getConnection();
             PreparedStatement ps = createEventsForAggregateTypePreparedStatement(con, aggregateType, afterOffset, batchSize);
             ResultSet rs = ps.executeQuery()) {

            ArrayList<StreamEvent<A, E>> persistedEvents = new ArrayList<>();

            while (rs.next()) {
                persistedEvents.add(resultSetToPersistedEvent(rs));
            }

            return List.ofAll(persistedEvents);
        }
    }

    private <A extends Aggregate<?, E, ?>, E extends DomainEvent> PreparedStatement createSaveEventsBatchedPreparedStatement(
            Connection connection,
            Class<A> aggregateType,
            AggregateId aggregateId,
            CommandId commandId,
            List<PersistedEvent<A, E>> events) throws SQLException {

        PreparedStatement statement = connection.prepareStatement("" +
                "INSERT INTO domain_event(aggregate_id, aggregate_type, command_id, event_type, event_payload, event_timestamp, sequence_number) " +
                "VALUES(?,?,?,?,?,?,?)");

        for (PersistedEvent<A, E> event : events) {
            statement.setString(1, aggregateId.get());
            statement.setString(2, aggregateType.getName()); // TODO: storing straight class name?
            statement.setString(3, commandId.get());
            statement.setString(4, event.rawEvent().getClass().getName()); // TODO: storing straight class name?
            statement.setString(5, mapper.serialise(event.rawEvent()));
            statement.setTimestamp(6, Timestamp.valueOf(event.timestamp()));
            statement.setLong(7, event.sequenceNumber());
            statement.addBatch();
        }

        return statement;
    }

    private <A extends Aggregate<?, E, ?>, E extends DomainEvent> PreparedStatement createSaveAggregatePreparedStatement(
            Connection connection,
            Class<A> aggregateType,
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
        statement.setString(2, aggregateType.getName()); // TODO: storing straight class name?
        statement.setLong(3, newVersion);
        statement.setLong(4, newVersion);
        statement.setLong(5, expectedPreviousVersion);

        return statement;
    }

    private <A extends Aggregate<?, E, ?>, E extends DomainEvent> PreparedStatement createEventsForAggregateInstancePreparedStatement(
            Connection connection,
            Class<A> aggregateType,
            AggregateId aggregateId,
            Option<Long> afterSequenceNumber) throws SQLException {

        PreparedStatement statement = connection.prepareStatement("" +
                "SELECT global_offset, aggregate_id, aggregate_type, command_id, event_type, event_payload, event_timestamp, sequence_number " +
                "FROM domain_event " +
                "WHERE aggregate_id = ? AND aggregate_type = ? AND sequence_number > ? " +
                "ORDER BY sequence_number");

        statement.setString(1, aggregateId.get());
        statement.setString(2, aggregateType.getName()); // TODO: storing straight class name?
        statement.setLong(3, afterSequenceNumber.getOrElse(-1L));

        return statement;
    }

    private <A extends Aggregate<?, E, ?>, E extends DomainEvent> PreparedStatement createEventsForAggregateTypePreparedStatement(
            Connection connection,
            Class<A> aggregateType,
            Option<Long> afterOffset,
            Integer batchSize) throws SQLException {

        PreparedStatement statement = connection.prepareStatement("" +
                "SELECT global_offset, aggregate_id, aggregate_type, command_id, event_type, event_payload, event_timestamp, sequence_number " +
                "FROM domain_event " +
                "WHERE aggregate_type = ? AND global_offset > ? " +
                "ORDER BY global_offset " +
                "LIMIT ?");

        statement.setString(1, aggregateType.getName()); // TODO: storing straight class name?
        statement.setLong(2, afterOffset.getOrElse(-1L));
        statement.setInt(3, batchSize);

        return statement;
    }

    private <A extends Aggregate<?, E, ?>, E extends DomainEvent> StreamEvent<A, E> resultSetToPersistedEvent(ResultSet rs)
            throws SQLException, ClassNotFoundException {
        Long offset = rs.getLong(1);
        AggregateId aggregateId = AggregateId.of(rs.getString(2));
        Class<A> aggregateType = (Class<A>) Class.forName(rs.getString(3));
        CommandId commandId = CommandId.of(rs.getString(4));
        Class<E> eventType = (Class<E>) Class.forName(rs.getString(5));
        E rawEvent = mapper.deserialise(rs.getString(6), eventType);
        LocalDateTime timestamp = rs.getTimestamp(7).toLocalDateTime();
        Long sequenceNumber = rs.getLong(8);
        return new PostgresStreamEvent<>(
                offset,
                aggregateId,
                aggregateType,
                commandId,
                rawEvent,
                timestamp,
                sequenceNumber);
    }

    private class PostgresStreamEvent<A extends Aggregate<?, E, ?>, E extends DomainEvent> extends PostgresEvent<A, E> implements StreamEvent<A, E> {

        private Long offset;

        public PostgresStreamEvent(
                Long offset,
                AggregateId aggregateId,
                Class<A> aggregateType,
                CommandId commandId,
                E rawEvent,
                LocalDateTime timestamp,
                Long sequenceNumber) {
            super(aggregateId, aggregateType, commandId, rawEvent, timestamp, sequenceNumber);
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

        private Class<A> aggregateType;

        private CommandId commandId;

        private E rawEvent;

        private LocalDateTime timestamp = LocalDateTime.now();

        private Long sequenceNumber;

        public PostgresEvent(
                AggregateId aggregateId,
                Class<A> aggregateType,
                CommandId commandId,
                E rawEvent,
                LocalDateTime timestamp,
                Long sequenceNumber) {

            this.aggregateId = aggregateId;
            this.aggregateType = aggregateType;
            this.commandId = commandId;
            this.rawEvent = rawEvent;
            this.timestamp = timestamp;
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
            return "PostgresEvent{" +
                    ", aggregateId=" + aggregateId +
                    ", aggregateType=" + aggregateType +
                    ", commandId=" + commandId +
                    ", rawEvent=" + rawEvent +
                    ", timestamp=" + timestamp +
                    ", sequenceNumber=" + sequenceNumber +
                    '}';
        }
    }
}
