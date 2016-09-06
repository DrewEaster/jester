package com.dreweaster.jester.infrastructure.driven.eventstore.mapper.json;

import com.dreweaster.jester.application.eventstore.EventPayloadMapper;
import com.dreweaster.jester.domain.DomainEvent;
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

// TODO: Refactor into separate child maven module
public class JsonEventPayloadMapper implements EventPayloadMapper {

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

    private Map<Tuple2<String, Integer>, Function1<String, DomainEvent>> deserialisers = HashMap.empty();

    private Map<String, Function1<DomainEvent, Tuple2<String, Integer>>> serialisers = HashMap.empty();

    public JsonEventPayloadMapper(ObjectMapper objectMapper, List<JsonEventMappingConfigurer<?>> mappers) {
        this.objectMapper = objectMapper;
        init(mappers);
    }

    @SuppressWarnings("unchecked")
    private void init(List<JsonEventMappingConfigurer<?>> configurers) {

        // TODO: Validate no clashes between registered mappers
        // e.g. what if two mappers try to convert to the same event class?
        // e.g. what if a migration in one mapper maps to a class name in another mapper?
        // Such scenarios should be made impossible (at least for v1...)

        List<MappingConfiguration> mappingConfigurations = configurers.map(configurer -> {
            MappingConfiguration mappingConfiguration = new MappingConfiguration();
            configurer.configure(mappingConfiguration);
            return mappingConfiguration;
        });

        deserialisers = mappingConfigurations.foldLeft(deserialisers, (acc, mappingConfiguration) ->
                acc.merge(mappingConfiguration.createDeserialisers()));

        serialisers = mappingConfigurations.foldLeft(serialisers, (acc, mappingConfiguration) ->
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
            currentClassName = initialEventClassName;
            currentVersion = 1;
            return this;
        }

        @Override
        public JsonEventMappingConfiguration<T> migrateFormat(Function1<JsonNode, JsonNode> migration) {
            migrations = migrations.append(new FormatMigration(currentClassName, currentVersion, currentVersion + 1, migration));
            currentVersion = currentVersion + 1;
            return this;
        }

        @Override
        public JsonEventMappingConfiguration<T> migrateClassName(String className) {
            Migration migration = new ClassNameMigration(currentClassName, className, currentVersion, currentVersion + 1);
            migrations = migrations.append(migration);
            currentClassName = migration.toClassName();
            currentVersion = migration.toVersion();
            return this;
        }

        @Override
        public void mappingFunctions(Function2<T, ObjectNode, JsonNode> serialiseFunction, Function1<JsonNode, T> deserialiseFunction) {
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
    public <T extends DomainEvent> T deserialise(
            String serialisedPayload,
            String serialisedEventType,
            Integer serialisedEventVersion) {

        Function1<String, DomainEvent> deserialiser = deserialisers
                .get(new Tuple2<>(serialisedEventType, serialisedEventVersion))
                .getOrElseThrow(() -> new MissingDeserialiserException(
                        serialisedEventType,
                        serialisedEventVersion));

        return (T) deserialiser.apply(serialisedPayload);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T extends DomainEvent> Tuple2<String, Integer> serialise(T event) {

        Function1<DomainEvent, Tuple2<String, Integer>> serialiser = serialisers
                .get(event.getClass().getName())
                .getOrElseThrow(() -> new MissingSerialiserException(event.getClass().getName()));

        return serialiser.apply(event);
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
