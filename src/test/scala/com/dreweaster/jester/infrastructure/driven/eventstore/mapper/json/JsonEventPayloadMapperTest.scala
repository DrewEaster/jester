package com.dreweaster.jester.infrastructure.driven.eventstore.mapper.json

import com.dreweaster.jester.domain.DomainEvent
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.ObjectNode
import org.scalatest.FeatureSpec

class JsonEventPayloadMapperTest extends FeatureSpec {

  scenario("A JsonEventPayloadMapper can deserialise events") {

    feature("Deserialise different versions a conceptual event with a complex migration history") {

    }

    feature("Throw MappingException when no deserialiser is found for an event type and version")
  }

  scenario("A JsonEventPayloadMapper can serialise events") {

  }
}

class EventWithComplexMigrationHistoryV8(val forename: String, val surname: String) extends DomainEvent

class EventWithComplexMigrationHistoryMappingConfigurer extends JsonEventMappingConfigurer[EventWithComplexMigrationHistoryV8] {

  def configure(configurationFactory: JsonEventMappingConfigurationFactory[EventWithComplexMigrationHistoryV8]) {
    configurationFactory.create("com.dreweaster.jester.infrastructure.driven.eventstore.mapper.json.EventWithComplexMigrationHistoryV1")
      .migrateFormat(migrateVersion1ToVersion2)
      .migrateFormat(migrateVersion2ToVersion3)
      .migrateFormat(migrateVersion3ToVersion4)
      .migrateClassName("com.dreweaster.jester.infrastructure.driven.eventstore.mapper.json.EventWithComplexMigrationHistoryV5")
      .migrateFormat(migrateVersion4ToVersion6)
      .migrateClassName(classOf[EventWithComplexMigrationHistoryV8].getName)
      .migrateFormat(migrateVersion6ToVersion8)
      .objectMappers(serialise, deserialise)
  }

  val serialise: javaslang.Function2[EventWithComplexMigrationHistoryV8, ObjectNode, JsonNode] =
    new javaslang.Function2[EventWithComplexMigrationHistoryV8, ObjectNode, JsonNode] {
      override def apply(event: EventWithComplexMigrationHistoryV8, root: ObjectNode): JsonNode = {
        root.put("forename", event.forename).put("surname", event.surname)
      }
    }

  val deserialise: javaslang.Function1[JsonNode, EventWithComplexMigrationHistoryV8] = new javaslang.Function1[JsonNode, EventWithComplexMigrationHistoryV8] {
    override def apply(root: JsonNode): EventWithComplexMigrationHistoryV8 =
      new EventWithComplexMigrationHistoryV8(
        forename = root.get("forename").asText,
        surname = root.get("surname").asText)
  }

  val migrateVersion1ToVersion2: javaslang.Function1[JsonNode, JsonNode] = new javaslang.Function1[JsonNode, JsonNode] {
    override def apply(node: JsonNode): JsonNode = node
  }

  def migrateVersion2ToVersion3: javaslang.Function1[JsonNode, JsonNode] = new javaslang.Function1[JsonNode, JsonNode] {
    override def apply(node: JsonNode): JsonNode = node
  }

  def migrateVersion3ToVersion4: javaslang.Function1[JsonNode, JsonNode] = new javaslang.Function1[JsonNode, JsonNode] {
    override def apply(node: JsonNode): JsonNode = node
  }

  def migrateVersion4ToVersion6: javaslang.Function1[JsonNode, JsonNode] = new javaslang.Function1[JsonNode, JsonNode] {
    override def apply(node: JsonNode): JsonNode = node
  }

  def migrateVersion6ToVersion8: javaslang.Function1[JsonNode, JsonNode] = new javaslang.Function1[JsonNode, JsonNode] {
    override def apply(node: JsonNode): JsonNode = node
  }
}
