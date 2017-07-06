package com.dreweaster.ddd.jester.infrastructure.driven.eventstore.mapper.json;

import com.dreweaster.ddd.jester.application.eventstore.PayloadMapper;
import com.dreweaster.ddd.jester.application.eventstore.SerialisationContentType;
import com.dreweaster.ddd.jester.domain.Aggregate;
import com.dreweaster.ddd.jester.domain.DomainEvent;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import javaslang.Function1;
import javaslang.Function2;
import javaslang.Tuple2;
import javaslang.collection.HashMap;
import javaslang.collection.List;
import javaslang.collection.Map;

import java.io.IOException;
import java.util.function.Function;

public class JsonPayloadMapper implements PayloadMapper {

    public static class InvalidMappingConfigurationException extends MappingException {

        public enum ConfigurationError {
            SERIALISE_FUNCTION,
            DESERIALISE_FUNCTION,
            INITIAL_CLASS_NAME,
            MIGRATION_CLASS_NAME,
            MIGRATION_FUNCTION
        }

        private ConfigurationError configurationError;

        public InvalidMappingConfigurationException(ConfigurationError error) {
            super("Invalid mapper configuration");
            this.configurationError = error;
        }

        public ConfigurationError configurationError() {
            return configurationError;
        }
    }

    public static class UnparseableJsonPayloadException extends MappingException {

        public UnparseableJsonPayloadException(Throwable cause, String serialisedPayload) {
            super("Could not parse JSON event payload: " + serialisedPayload, cause);
        }
    }

    public static class MissingDeserialiserException extends MappingException {

        public MissingDeserialiserException(String serialisedEventType, Integer serialisedEventVersion) {
            super(String.format(
                    "No deserialiser found for event_type = '%s' with event_version = '%d'",
                    serialisedEventType,
                    serialisedEventVersion));
        }
    }

    public static class MissingSerialiserException extends MappingException {
        public MissingSerialiserException(String eventType) {
            super(String.format("No serialiser found for event_type = '%s'", eventType));
        }
    }

    private ObjectMapper objectMapper;

    private Map<Tuple2<String, Integer>, Function1<String, DomainEvent>> eventDeserialisers = HashMap.empty();

    private Map<String, Function1<DomainEvent, Tuple2<String, Integer>>> eventSerialisers = HashMap.empty();

    private Map<Class<?>, StatePayloadJsonSerialiser<?,?>> stateSerialisers = HashMap.empty();

    public JsonPayloadMapper(
            ObjectMapper objectMapper,
            List<JsonEventMappingConfigurer<?>> eventMappers,
            List<StatePayloadJsonSerialiser<?,?>> stateSerialisers) {
        this.objectMapper = objectMapper;

        // Prepare event mappers
        init(eventMappers);

        // Prepare state serialisers
        this.stateSerialisers = stateSerialisers.foldLeft(
                HashMap.<Class<?>, StatePayloadJsonSerialiser<?,?>>empty(),
                (acc,item) -> acc.put(item.stateClass(), item));
    }

    @SuppressWarnings("unchecked")
    private void init(List<JsonEventMappingConfigurer<?>> configurers) {

        // TODO: Validate no clashes between registered mappers
        // e.g. what if two mappers try to convert to the same event class?
        // e.g. what if a com.dreweaster.ddd.jester.infrastructure.driven.eventstore.com.dreweaster.ddd.jester.infrastructure.driven.eventstore.postgres.db.migration in one mapper maps to a class name in another mapper?
        // Such scenarios should be made impossible (at least for v1...)

        List<MappingConfiguration> mappingConfigurations = configurers.map(configurer -> {
            MappingConfiguration mappingConfiguration = new MappingConfiguration();
            configurer.configure(mappingConfiguration);
            return mappingConfiguration;
        });

        eventDeserialisers = mappingConfigurations.foldLeft(eventDeserialisers, (acc, mappingConfiguration) ->
                acc.merge(mappingConfiguration.createDeserialisers()));

        eventSerialisers = mappingConfigurations.foldLeft(eventSerialisers, (acc, mappingConfiguration) ->
                acc.put(mappingConfiguration.createSerialiser()));
    }

    private class MappingConfiguration<T extends DomainEvent> implements JsonEventMappingConfigurationFactory<T>, JsonEventMappingConfiguration<T> {

        private int currentVersion;

        private String currentClassName;

        private List<Migration> migrations = List.empty();

        private Function2<T, ObjectNode, JsonNode> serialiseFunction;

        private Function1<JsonNode, T> deserialiseFunction;

        @Override
        public JsonEventMappingConfiguration<T> create(String initialEventClassName) {
            if (initialEventClassName == null) {
                throw new InvalidMappingConfigurationException(InvalidMappingConfigurationException.ConfigurationError.INITIAL_CLASS_NAME);
            }
            currentClassName = initialEventClassName;
            currentVersion = 1;
            return this;
        }

        @Override
        public JsonEventMappingConfiguration<T> migrateFormat(Function1<JsonNode, JsonNode> migration) {
            if (migration == null) {
                throw new InvalidMappingConfigurationException(InvalidMappingConfigurationException.ConfigurationError.MIGRATION_FUNCTION);
            }

            migrations = migrations.append(new FormatMigration(currentClassName, currentVersion, currentVersion + 1, migration));
            currentVersion = currentVersion + 1;
            return this;
        }

        @Override
        public JsonEventMappingConfiguration<T> migrateClassName(String className) {
            if (className == null) {
                throw new InvalidMappingConfigurationException(InvalidMappingConfigurationException.ConfigurationError.MIGRATION_CLASS_NAME);
            }

            Migration migration = new ClassNameMigration(currentClassName, className, currentVersion, currentVersion + 1);
            migrations = migrations.append(migration);
            currentClassName = migration.toClassName();
            currentVersion = migration.toVersion();
            return this;
        }

        @Override
        public void mappingFunctions(Function2<T, ObjectNode, JsonNode> serialiseFunction, Function1<JsonNode, T> deserialiseFunction) {
            if (serialiseFunction == null) {
                throw new InvalidMappingConfigurationException(InvalidMappingConfigurationException.ConfigurationError.SERIALISE_FUNCTION);
            }

            if (deserialiseFunction == null) {
                throw new InvalidMappingConfigurationException(InvalidMappingConfigurationException.ConfigurationError.DESERIALISE_FUNCTION);
            }

            this.serialiseFunction = serialiseFunction;
            this.deserialiseFunction = deserialiseFunction;
        }

        private Map<Tuple2<String, Integer>, Function1<String, DomainEvent>> createDeserialisers() {
            Map<Tuple2<String, Integer>, Function1<String, DomainEvent>> deserialisers = HashMap.empty();

            if (!migrations.isEmpty()) {
                deserialisers = putDeserialisers(migrations, deserialisers);
            }

            // Include the 'current' version deserialiser
            deserialisers = deserialisers.put(new Tuple2<>(currentClassName, currentVersion), serialisedEvent -> {
                JsonNode root = stringToJsonNode(serialisedEvent);
                return deserialiseFunction.apply(root);
            });
            return deserialisers;
        }

        @SuppressWarnings("unchecked")
        private Tuple2<String, Function1<DomainEvent, Tuple2<String, Integer>>> createSerialiser() {
            return new Tuple2<>(currentClassName, domainEvent -> {
                ObjectNode root = objectMapper.createObjectNode();
                JsonNode serialisedJsonEvent = serialiseFunction.apply((T) domainEvent, root);
                return new Tuple2<>(serialisedJsonEvent.toString(), currentVersion);
            });
        }

        private Map<Tuple2<String, Integer>, Function1<String, DomainEvent>> putDeserialisers(
                List<Migration> migrations,
                Map<Tuple2<String, Integer>, Function1<String, DomainEvent>> deserialisers) {

            if (migrations.isEmpty()) {
                return deserialisers;
            } else {
                return putDeserialisers(migrations.tail(), putDeserialiser(migrations, deserialisers));
            }
        }

        private Map<Tuple2<String, Integer>, Function1<String, DomainEvent>> putDeserialiser(
                List<Migration> migrations,
                Map<Tuple2<String, Integer>, Function1<String, DomainEvent>> deserialisers) {

            Migration migration = migrations.head();
            String className = migration.fromClassName();
            Integer version = migration.fromVersion();
            List<Function<JsonNode, JsonNode>> migrationFunctions = migrations.map(Migration::migrationFunction);
            Function<JsonNode, JsonNode> combinedMigrationFunction = migrationFunctions
                    .tail()
                    .foldLeft(migrationFunctions.head(), (combined, f) -> f.compose(combined));

            Function1<String, DomainEvent> deserialiser = serialisedEvent -> {
                JsonNode root = stringToJsonNode(serialisedEvent);
                JsonNode migratedRoot = combinedMigrationFunction.apply(root);
                return deserialiseFunction.apply(migratedRoot);
            };

            return deserialisers.put(new Tuple2<>(className, version), deserialiser);
        }

        private JsonNode stringToJsonNode(String serialisedEvent) {
            try {
                return objectMapper.readTree(serialisedEvent);
            } catch (IOException ex) {
                throw new UnparseableJsonPayloadException(ex, serialisedEvent);
            }
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T extends DomainEvent> T deserialiseEvent(
            String serialisedPayload,
            String serialisedEventType,
            Integer serialisedEventVersion) {

        Function1<String, DomainEvent> deserialiser = eventDeserialisers
                .get(new Tuple2<>(serialisedEventType, serialisedEventVersion))
                .getOrElseThrow(() -> new MissingDeserialiserException(
                        serialisedEventType,
                        serialisedEventVersion));

        return (T) deserialiser.apply(serialisedPayload);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T extends DomainEvent> PayloadSerialisationResult serialiseEvent(T event) {

        Function1<DomainEvent, Tuple2<String, Integer>> serialiser = eventSerialisers
                .get(event.getClass().getName())
                .getOrElseThrow(() -> new MissingSerialiserException(event.getClass().getName()));

        Tuple2<String,Integer> versionedPayload = serialiser.apply(event);

        return PayloadSerialisationResult.of(
                versionedPayload._1,
                SerialisationContentType.JSON,
                versionedPayload._2
        );
    }

    @SuppressWarnings("unchecked")
    @Override
    public <A extends Aggregate<?, ?, State>, State> PayloadSerialisationResult serialiseState(State state) {
        return ((StatePayloadJsonSerialiser<A,State>)stateSerialisers
                .get(state.getClass()).getOrElseThrow(() -> new MissingSerialiserException(state.getClass().getName())))
                .serialise(state, objectMapper.createObjectNode());
    }

    private interface Migration {

        String fromClassName();

        String toClassName();

        Integer fromVersion();

        Integer toVersion();

        Function1<JsonNode, JsonNode> migrationFunction();
    }

    private class FormatMigration implements Migration {

        private String className;

        private Integer fromVersion;

        private Integer toVersion;

        private Function1<JsonNode, JsonNode> migrationFunction;

        public FormatMigration(
                String className,
                Integer fromVersion,
                Integer toVersion,
                Function1<JsonNode, JsonNode> migrationFunction) {

            this.className = className;
            this.fromVersion = fromVersion;
            this.toVersion = toVersion;
            this.migrationFunction = migrationFunction;
        }

        @Override
        public String fromClassName() {
            return className;
        }

        @Override
        public String toClassName() {
            return className;
        }

        @Override
        public Integer fromVersion() {
            return fromVersion;
        }

        @Override
        public Integer toVersion() {
            return toVersion;
        }

        @Override
        public Function1<JsonNode, JsonNode> migrationFunction() {
            return migrationFunction;
        }
    }

    private class ClassNameMigration implements Migration {

        private String fromClassName;

        private String toClassName;

        private Integer fromVersion;

        private Integer toVersion;

        public ClassNameMigration(
                String fromClassName,
                String toClassName,
                Integer fromVersion,
                Integer toVersion) {

            this.fromClassName = fromClassName;
            this.toClassName = toClassName;
            this.fromVersion = fromVersion;
            this.toVersion = toVersion;
        }

        @Override
        public String fromClassName() {
            return fromClassName;
        }

        @Override
        public String toClassName() {
            return toClassName;
        }

        @Override
        public Integer fromVersion() {
            return fromVersion;
        }

        @Override
        public Integer toVersion() {
            return toVersion;
        }

        @Override
        public Function1<JsonNode, JsonNode> migrationFunction() {
            return (jsonNode -> jsonNode);
        }
    }
}
