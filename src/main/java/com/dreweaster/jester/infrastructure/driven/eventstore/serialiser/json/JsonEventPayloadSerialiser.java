package com.dreweaster.jester.infrastructure.driven.eventstore.serialiser.json;

import com.dreweaster.jester.application.eventstore.EventPayloadSerialiser;
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

// TODO: Refactor into separate child maven module
public class JsonEventPayloadSerialiser implements EventPayloadSerialiser {

    private ObjectMapper objectMapper;

    private Map<Tuple2<String, Integer>, Function1<String, DomainEvent>> deserialisers = HashMap.empty();

    private Map<String, Function1<DomainEvent, Tuple2<String, Integer>>> serialisers = HashMap.empty();

    public JsonEventPayloadSerialiser(ObjectMapper objectMapper, List<JsonEventMapper<?>> mappers) {
        this.objectMapper = objectMapper;
        init(mappers);
    }

    @SuppressWarnings("unchecked")
    private void init(List<JsonEventMapper<?>> mappers) {
        deserialisers = mappers.foldLeft(deserialisers, (acc, mapper) -> {
            MappingConfiguration mappingConfiguration = new MappingConfiguration();
            mapper.configure(mappingConfiguration);
            return acc.merge(mappingConfiguration.createDeserialisers());
        });
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
            migrations = migrations.append(new ClassNameMigration(className, currentVersion, currentVersion + 1));
            currentClassName = className;
            currentVersion = currentVersion + 1;
            return this;
        }

        @Override
        public void mapper(Function2<T, ObjectNode, JsonNode> serialiseFunction, Function1<JsonNode, T> deserialiseFunction) {
            this.serialiseFunction = serialiseFunction;
            this.deserialiseFunction = deserialiseFunction;
        }

        private Map<Tuple2<String, Integer>, Function1<String, DomainEvent>> createDeserialisers() {
            return null;
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
                .getOrElseThrow(() -> new MappingException(String.format(
                        "No deserialiser found for event_type = '%s' with event_version = '%d'",
                        serialisedEventType,
                        serialisedEventVersion)));

        return (T) deserialiser.apply(serialisedPayload);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T extends DomainEvent> Tuple2<String, Integer> serialise(T event) {

        Function1<DomainEvent, Tuple2<String, Integer>> serialiser = serialisers
                .get(event.getClass().getName())
                .getOrElseThrow(() -> new MappingException(String.format(
                        "No serialiser found for event_type = '%s'"
                        , event.getClass().getName())));

        return serialiser.apply(event);
    }

    private interface Migration {

        String className();

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
        public String className() {
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
        private String className;

        private Integer fromVersion;

        private Integer toVersion;

        public ClassNameMigration(
                String className,
                Integer fromVersion,
                Integer toVersion) {

            this.className = className;
            this.fromVersion = fromVersion;
            this.toVersion = toVersion;
        }

        @Override
        public String className() {
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
            return (jsonNode -> jsonNode);
        }
    }
}
