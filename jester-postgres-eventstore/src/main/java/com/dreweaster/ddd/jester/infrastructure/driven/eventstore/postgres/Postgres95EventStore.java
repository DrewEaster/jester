package com.dreweaster.ddd.jester.infrastructure.driven.eventstore.postgres;

import com.dreweaster.ddd.jester.application.eventstore.*;
import com.dreweaster.ddd.jester.application.eventstore.PayloadMapper.PayloadSerialisationResult;
import com.dreweaster.ddd.jester.domain.*;
import javaslang.Tuple2;
import javaslang.collection.List;
import javaslang.concurrent.Future;
import javaslang.control.Option;
import org.postgresql.util.PGobject;

import javax.sql.DataSource;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.concurrent.ExecutorService;

/**
 * TODO: Implement integration tests using Postgres started by Docker (via Maven).
 */
public class Postgres95EventStore implements EventStore {

    private DataSource dataSource;

    private ExecutorService executorService;

    private PayloadMapper payloadMapper;

    private DomainModel domainModel;

    public Postgres95EventStore(
            DataSource dataSource,
            ExecutorService executorService,
            PayloadMapper payloadMapper,
            DomainModel domainModel) {
        this.dataSource = dataSource;
        this.executorService = executorService;
        this.payloadMapper = payloadMapper;
        this.domainModel = domainModel;
    }

    @Override
    public <A extends Aggregate<?, E, State>, E extends DomainEvent, State> Future<List<PersistedEvent<A, E>>> loadEvents(
            AggregateType<A, ?, E, State> aggregateType, AggregateId aggregateId) {
        return Future.of(executorService, () -> loadEventsForAggregateInstance(aggregateType, aggregateId, Option.none()));
    }

    @Override
    public <A extends Aggregate<?, E, State>, E extends DomainEvent, State> Future<List<PersistedEvent<A, E>>> loadEvents(
            AggregateType<A, ?, E, State> aggregateType, AggregateId aggregateId, Long afterSequenceNumber) {
        return Future.of(executorService, () -> loadEventsForAggregateInstance(
                aggregateType,
                aggregateId,
                Option.of(afterSequenceNumber)));
    }

    @Override
    public <A extends Aggregate<?, E, State>, E extends DomainEvent, State> Future<List<StreamEvent<A, E>>> loadEventStream(
            AggregateType<A, ?, E, State> aggregateType,
            Integer batchSize) {

        return Future.of(executorService, () -> loadEventsForAggregateType(
                aggregateType,
                Option.none(),
                batchSize));
    }

    @Override
    public <A extends Aggregate<?, E, State>, E extends DomainEvent, State> Future<List<StreamEvent<A, E>>> loadEventStream(
            AggregateType<A, ?, E, State> aggregateType,
            Long afterOffset,
            Integer batchSize) {

        return Future.of(executorService, () -> loadEventsForAggregateType(
                aggregateType,
                Option.of(afterOffset),
                batchSize));
    }

    @Override
    public <E extends DomainEvent> Future<List<StreamEvent<?, E>>> loadEventStream(DomainEventTag tag, Long afterOffset, Integer batchSize) {
        return Future.of(executorService, () -> loadEventsForTag(
                tag,
                Option.of(afterOffset),
                batchSize));
    }

    @Override
    public <A extends Aggregate<?, E, State>, E extends DomainEvent, State> Future<List<PersistedEvent<A, E>>> saveEvents(
            AggregateType<A, ?, E, State> aggregateType,
            AggregateId aggregateId,
            CausationId causationId,
            List<E> rawEvents,
            Long expectedSequenceNumber) {
        return Future.of(executorService, () -> saveEventsForAggregateInstance(
                aggregateType,
                aggregateId,
                causationId,
                Option.none(),
                rawEvents,
                Option.none(),
                expectedSequenceNumber));
    }

    @Override
    public <A extends Aggregate<?, E, State>, E extends DomainEvent, State> Future<List<PersistedEvent<A, E>>> saveEventsAndState(
            AggregateType<A, ?, E, State> aggregateType,
            AggregateId aggregateId,
            CausationId causationId,
            List<E> rawEvents,
            State state,
            Long expectedSequenceNumber) {
        return Future.of(executorService, () -> saveEventsForAggregateInstance(
                aggregateType,
                aggregateId,
                causationId,
                Option.none(),
                rawEvents,
                Option.of(state),
                expectedSequenceNumber)
        );
    }

    @Override
    public <A extends Aggregate<?, E, State>, E extends DomainEvent, State> Future<List<PersistedEvent<A, E>>> saveEvents(
            AggregateType<A, ?, E, State> aggregateType,
            AggregateId aggregateId,
            CausationId causationId,
            CorrelationId correlationId,
            List<E> rawEvents,
            Long expectedSequenceNumber) {
        return Future.of(executorService, () -> saveEventsForAggregateInstance(
                aggregateType,
                aggregateId,
                causationId,
                Option.of(correlationId),
                rawEvents,
                Option.none(),
                expectedSequenceNumber));
    }

    @Override
    public <A extends Aggregate<?, E, State>, E extends DomainEvent, State> Future<List<PersistedEvent<A, E>>> saveEventsAndState(
            AggregateType<A, ?, E, State> aggregateType,
            AggregateId aggregateId,
            CausationId causationId,
            CorrelationId correlationId,
            List<E> rawEvents,
            State state,
            Long expectedSequenceNumber) {
        return Future.of(executorService, () -> saveEventsForAggregateInstance(
                aggregateType,
                aggregateId,
                causationId,
                Option.of(correlationId),
                rawEvents,
                Option.of(state),
                expectedSequenceNumber)
        );
    }

    private <A extends Aggregate<?, E, State>, E extends DomainEvent, State> List<PersistedEvent<A, E>> saveEventsForAggregateInstance(
            AggregateType<A, ?, E, State> aggregateType,
            AggregateId aggregateId,
            CausationId causationId,
            Option<CorrelationId> correlationId,
            List<E> rawEvents,
            Option<State> state,
            Long expectedSequenceNumber) throws SQLException {

        // TODO: What would sequence number be if no previous events had been saved?

        LocalDateTime timestamp = LocalDateTime.now(); // TODO: Inject clock

        List<PersistedEvent<A, E>> persistedEvents = rawEvents.foldLeft(
                new Tuple2<Long, List<PersistedEvent<A, E>>>(expectedSequenceNumber + 1, List.empty()), (acc, event) -> {

                    PayloadSerialisationResult serialisationResult = payloadMapper.serialiseEvent(event);

                    return new Tuple2<>(acc._1 + 1, acc._2.append(
                            new PostgresEvent<>(
                                    EventId.createUnique(),
                                    aggregateId,
                                    aggregateType,
                                    causationId,
                                    correlationId,
                                    event,
                                    serialisationResult.payload(),
                                    serialisationResult.version().get(),
                                    timestamp,
                                    acc._1)));
                })._2;

        Long latestSequenceNumber = persistedEvents.last().sequenceNumber();

        try (Connection con = dataSource.getConnection();
             PreparedStatement seps = createSaveEventsBatchedPreparedStatement(con, aggregateType, aggregateId, causationId, correlationId, persistedEvents);
             PreparedStatement saps = createSaveAggregatePreparedStatement(con, aggregateType, aggregateId, state, latestSequenceNumber, expectedSequenceNumber)) {

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
    private <A extends Aggregate<?, E, State>, E extends DomainEvent, State> List<PersistedEvent<A, E>> loadEventsForAggregateInstance(
            AggregateType<A, ?, E, ?> aggregateType,
            AggregateId aggregateId,
            Option<Long> afterSequenceNumber) throws SQLException, ClassNotFoundException {

        try (Connection con = dataSource.getConnection();
             PreparedStatement ps = createEventsForAggregateInstancePreparedStatement(con, aggregateType, aggregateId, afterSequenceNumber);
             ResultSet rs = ps.executeQuery()) {

            ArrayList<PersistedEvent<A, E>> persistedEvents = new ArrayList<>();

            while (rs.next()) {
                persistedEvents.add(resultSetToPersistedEvent(rs));
            }

            con.commit();
            return List.ofAll(persistedEvents);
        }
    }

    @SuppressWarnings("unchecked")
    private <A extends Aggregate<?, E, State>, E extends DomainEvent, State> List<StreamEvent<A, E>> loadEventsForAggregateType(
            AggregateType<A, ?, E, ?> aggregateType,
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

    @SuppressWarnings("unchecked")
    private <A extends Aggregate<?, E, State>, E extends DomainEvent, State> List<StreamEvent<?, E>> loadEventsForTag(
            DomainEventTag tag,
            Option<Long> afterOffset,
            Integer batchSize) throws SQLException, ClassNotFoundException {

        try (Connection con = dataSource.getConnection();
             PreparedStatement ps = createEventsForTagPreparedStatement(con, tag, afterOffset, batchSize);
             ResultSet rs = ps.executeQuery()) {

            ArrayList<StreamEvent<A, E>> persistedEvents = new ArrayList<>();

            while (rs.next()) {
                persistedEvents.add(resultSetToPersistedEvent(rs));
            }

            return List.ofAll(persistedEvents);
        }
    }

    @SuppressWarnings("unchecked")
    private <A extends Aggregate<?, E, ?>, E extends DomainEvent> PreparedStatement createSaveEventsBatchedPreparedStatement(
            Connection connection,
            AggregateType<A, ?, E, ?> aggregateType,
            AggregateId aggregateId,
            CausationId causationId,
            Option<CorrelationId> correlationId,
            List<PersistedEvent<A, E>> events) throws SQLException {

        PreparedStatement statement = connection.prepareStatement("" +
                "INSERT INTO domain_event(event_id, aggregate_id, aggregate_type, tag, causation_id, correlation_id, event_type, event_version, event_payload, event_timestamp, sequence_number) " +
                "VALUES(?,?,?,?,?,?,?,?,?,?,?)");

        for (PersistedEvent<A, E> event : events) {
            statement.setString(1, event.id().get());
            statement.setString(2, aggregateId.get());
            statement.setString(3, aggregateType.name());
            statement.setString(4, event.rawEvent().tag().tag());
            statement.setString(5, causationId.get());
            if (correlationId.isDefined()) {
                statement.setString(6, correlationId.get().get());
            } else {
                statement.setString(6, null);
            }
            statement.setString(7, event.rawEvent().getClass().getName());
            statement.setInt(8, event.eventVersion());
            statement.setString(9, ((PostgresEvent<A, E>) event).serialisedEvent()); // TODO: Suspicious casting :-)
            statement.setTimestamp(10, Timestamp.valueOf(event.timestamp()));
            statement.setLong(11, event.sequenceNumber());
            statement.addBatch();
        }

        return statement;
    }

    @SuppressWarnings("unchecked")
    private <A extends Aggregate<?, E, State>, E extends DomainEvent, State> PreparedStatement createSaveAggregatePreparedStatement(
            Connection connection,
            AggregateType<A, ?, E, State> aggregateType,
            AggregateId aggregateId,
            Option<State> state,
            Long newVersion,
            Long expectedPreviousVersion) throws SQLException {

        // TODO: Need to test behaviour and expectations for case when there are no previous events. What should caller send as expectedPreviousVersion?
        if(state.isEmpty()) {
            return createSaveAggregatePreparedStatementWithoutState(connection, aggregateType, aggregateId, newVersion, expectedPreviousVersion);
        } else {
            return createSaveAggregatePreparedStatementWithState(connection, aggregateType, aggregateId, state.get(), newVersion, expectedPreviousVersion);
        }
    }

    private <A extends Aggregate<?, E, State>, E extends DomainEvent, State> PreparedStatement createSaveAggregatePreparedStatementWithoutState(
            Connection connection,
            AggregateType<A, ?, E, State> aggregateType,
            AggregateId aggregateId,
            Long newVersion,
            Long expectedPreviousVersion) throws SQLException {

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

    private <A extends Aggregate<?, E, State>, E extends DomainEvent, State> PreparedStatement createSaveAggregatePreparedStatementWithState(
            Connection connection,
            AggregateType<A, ?, E, State> aggregateType,
            AggregateId aggregateId,
            State state,
            Long newVersion,
            Long expectedPreviousVersion) throws SQLException {

        PreparedStatement statement = connection.prepareStatement("" +
                "INSERT INTO aggregate_root (aggregate_id,aggregate_type,aggregate_version,aggregate_state) " +
                "VALUES (?,?,?,?) " +
                "ON CONFLICT ON CONSTRAINT aggregate_root_pkey " +
                "DO UPDATE SET aggregate_version = ? WHERE aggregate_root.aggregate_version = ?");

        statement.setString(1, aggregateId.get());
        statement.setString(2, aggregateType.name());
        statement.setLong(3, newVersion);

        // TODO: Assumes that the state is JSON!!!
        PGobject jsonObject = new PGobject();
        jsonObject.setType("jsonb");
        jsonObject.setValue(payloadMapper.serialiseState(state).payload());
        statement.setObject(4, jsonObject);

        statement.setLong(5, newVersion);
        statement.setLong(6, expectedPreviousVersion);

        return statement;
    }

    private <A extends Aggregate<?, E, State>, E extends DomainEvent, State> PreparedStatement createEventsForAggregateInstancePreparedStatement(
            Connection connection,
            AggregateType<A, ?, E, ?> aggregateType,
            AggregateId aggregateId,
            Option<Long> afterSequenceNumber) throws SQLException {

        PreparedStatement statement = connection.prepareStatement("" +
                "SELECT global_offset, event_id, aggregate_id, aggregate_type, causation_id, correlation_id, event_type, event_version, event_payload, event_timestamp, sequence_number " +
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
                "SELECT global_offset, event_id, aggregate_id, aggregate_type, causation_id, correlation_id, event_type, event_version, event_payload, event_timestamp, sequence_number " +
                "FROM domain_event " +
                "WHERE aggregate_type = ? AND global_offset > ? " +
                "ORDER BY global_offset " +
                "LIMIT ?");

        statement.setString(1, aggregateType.name());
        statement.setLong(2, afterOffset.getOrElse(-1L));
        statement.setInt(3, batchSize);

        return statement;
    }

    private <A extends Aggregate<?, E, State>, E extends DomainEvent, State> PreparedStatement createEventsForTagPreparedStatement(
            Connection connection,
            DomainEventTag tag,
            Option<Long> afterOffset,
            Integer batchSize) throws SQLException {

        PreparedStatement statement = connection.prepareStatement("" +
                "SELECT global_offset, event_id, aggregate_id, aggregate_type, causation_id, correlation_id, event_type, event_version, event_payload, event_timestamp, sequence_number " +
                "FROM domain_event " +
                "WHERE tag = ? AND global_offset > ? " +
                "ORDER BY global_offset " +
                "LIMIT ?");

        statement.setString(1, tag.tag());
        statement.setLong(2, afterOffset.getOrElse(-1L));
        statement.setInt(3, batchSize);

        return statement;
    }

    @SuppressWarnings("unchecked")
    private <A extends Aggregate<?, E, State>, E extends DomainEvent, State> StreamEvent<A, E> resultSetToPersistedEvent(
            ResultSet rs)
            throws SQLException, ClassNotFoundException {
        Long offset = rs.getLong(1);
        EventId eventId = EventId.of(rs.getString(2));
        AggregateId aggregateId = AggregateId.of(rs.getString(3));
        String aggregateTypeString = rs.getString(4);
        AggregateType<A, ?, E, ?> aggregateType = (AggregateType<A, ?, E, ?>) domainModel.aggregateTypeFor(aggregateTypeString);
        CausationId causationId = CausationId.of(rs.getString(5));
        Option<CorrelationId> correlationId = CorrelationId.ofNullable(rs.getString(6));
        String eventType = rs.getString(7);
        Integer eventVersion = rs.getInt(8);
        String serialisedEvent = rs.getString(9);
        E rawEvent = payloadMapper.deserialiseEvent(serialisedEvent, eventType, eventVersion);
        LocalDateTime timestamp = rs.getTimestamp(10).toLocalDateTime();
        Long sequenceNumber = rs.getLong(11);
        return new PostgresStreamEvent<>(
                offset,
                eventId,
                aggregateId,
                aggregateType,
                causationId,
                correlationId,
                rawEvent,
                serialisedEvent,
                eventVersion,
                timestamp,
                sequenceNumber);
    }

    private class PostgresStreamEvent<A extends Aggregate<?, E, State>, E extends DomainEvent, State> extends PostgresEvent<A, E> implements StreamEvent<A, E> {

        private Long offset;

        public PostgresStreamEvent(
                Long offset,
                EventId eventId,
                AggregateId aggregateId,
                AggregateType<A, ?, E, ?> aggregateType,
                CausationId causationId,
                Option<CorrelationId> correlationId,
                E rawEvent,
                String serialisedEvent,
                Integer eventVersion,
                LocalDateTime timestamp,
                Long sequenceNumber) {
            super(eventId, aggregateId, aggregateType, causationId, correlationId, rawEvent, serialisedEvent, eventVersion, timestamp, sequenceNumber);
            this.offset = offset;
        }

        @Override
        public Long offset() {
            return offset;
        }
    }

    private class PostgresEvent<A extends Aggregate<?, E, ?>, E extends DomainEvent>
            implements PersistedEvent<A, E> {

        private EventId eventId;

        private AggregateId aggregateId;

        private AggregateType<A, ?, E, ?> aggregateType;

        private CausationId causationId;

        private Option<CorrelationId> correlationId;

        private E rawEvent;

        private String serialisedEvent;

        private Integer eventVersion;

        private LocalDateTime timestamp = LocalDateTime.now();

        private Long sequenceNumber;

        public PostgresEvent(
                EventId eventId,
                AggregateId aggregateId,
                AggregateType<A, ?, E, ?> aggregateType,
                CausationId causationId,
                Option<CorrelationId> correlationId,
                E rawEvent,
                String serialisedEvent,
                Integer eventVersion,
                LocalDateTime timestamp,
                Long sequenceNumber) {

            this.eventId = eventId;
            this.aggregateId = aggregateId;
            this.aggregateType = aggregateType;
            this.causationId = causationId;
            this.correlationId = correlationId;
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
        public EventId id() {
            return eventId;
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
                    "eventId=" + eventId +
                    ", aggregateId=" + aggregateId +
                    ", aggregateType=" + aggregateType +
                    ", tag=" + rawEvent().tag().tag() +
                    ", causationId=" + causationId +
                    ", correlationId=" + correlationId +
                    ", rawEvent=" + rawEvent +
                    ", serialisedEvent='" + serialisedEvent + '\'' +
                    ", eventVersion=" + eventVersion +
                    ", timestamp=" + timestamp +
                    ", sequenceNumber=" + sequenceNumber +
                    '}';
        }
    }
}
