package com.dreweaster.ddd.jester.infrastructure.driven.eventstore.mapper.json

import com.dreweaster.ddd.jester.domain.{DomainEvent, DomainEventTag}
import JsonPayloadMapper.InvalidMappingConfigurationException.ConfigurationError._
import JsonPayloadMapper.{InvalidMappingConfigurationException, MissingDeserialiserException}
import com.fasterxml.jackson.databind.node.ObjectNode
import com.fasterxml.jackson.databind.{JsonNode, ObjectMapper}
import org.scalatest.{FeatureSpec, GivenWhenThen, Matchers}

class JsonPayloadMapperTest extends FeatureSpec with GivenWhenThen with Matchers {

  val objectMapper = new ObjectMapper()

  feature("A JsonPayloadMapper can deserialiseEvent different versions of a conceptual event with a complex com.dreweaster.ddd.jester.infrastructure.driven.eventstore.com.dreweaster.ddd.jester.infrastructure.driven.eventstore.postgres.db.migration history") {

    val configurers: javaslang.collection.List[JsonEventMappingConfigurer[_]] = javaslang.collection.List.empty().append(
      new EventWithComplexMigrationHistoryMappingConfigurer
    )

    val stateSerialisers: javaslang.collection.List[StatePayloadJsonSerialiser[_,_]] = javaslang.collection.List.empty()

    val payloadMapper = new JsonPayloadMapper(objectMapper, configurers, stateSerialisers)

    scenario("Deserialise a version 1 event") {
      Given("a version 1 payload")
      val eventVersion1Payload = objectMapper.createObjectNode()
        .put("firstName", "joe")
        .put("secondName", "bloggs")
        .toString

      When("deserialising the payload")
      val event = payloadMapper.deserialiseEvent(
        eventVersion1Payload,
        "com.dreweaster.jester.infrastructure.driven.eventstore.mapper.json.EventWithComplexMigrationHistoryClassName1",
        1).asInstanceOf[EventWithComplexMigrationHistoryClassName3]

      Then("the event should be deserialised into version 8")
      event.forename should be("joe")
      event.surname should be("bloggs")
      event.active should be(true)
    }

    scenario("Deserialise a version 2 event") {
      Given("a version 2 payload")
      val eventVersion2Payload = objectMapper.createObjectNode()
        .put("first_name", "joe")
        .put("second_name", "bloggs")
        .toString

      When("deserialising the payload")
      val event = payloadMapper.deserialiseEvent(
        eventVersion2Payload,
        "com.dreweaster.jester.infrastructure.driven.eventstore.mapper.json.EventWithComplexMigrationHistoryClassName1",
        2).asInstanceOf[EventWithComplexMigrationHistoryClassName3]

      Then("the event should be deserialised into version 8")
      event.forename should be("joe")
      event.surname should be("bloggs")
      event.active should be(true)
    }

    scenario("Deserialise a version 3 event") {
      Given("a version 3 payload")
      val eventVersion3Payload = objectMapper.createObjectNode()
        .put("first_name", "joe")
        .put("last_name", "bloggs")
        .toString

      When("deserialising the payload")
      val event = payloadMapper.deserialiseEvent(
        eventVersion3Payload,
        "com.dreweaster.jester.infrastructure.driven.eventstore.mapper.json.EventWithComplexMigrationHistoryClassName1",
        3).asInstanceOf[EventWithComplexMigrationHistoryClassName3]

      Then("the event should be deserialised into version 8")
      event.forename should be("joe")
      event.surname should be("bloggs")
      event.active should be(true)
    }

    scenario("Deserialise a version 4 event") {
      Given("a version 3 payload")
      val eventVersion4Payload = objectMapper.createObjectNode()
        .put("forename", "joe")
        .put("surname", "bloggs")
        .toString

      When("deserialising the payload")
      val event = payloadMapper.deserialiseEvent(
        eventVersion4Payload,
        "com.dreweaster.jester.infrastructure.driven.eventstore.mapper.json.EventWithComplexMigrationHistoryClassName1",
        4).asInstanceOf[EventWithComplexMigrationHistoryClassName3]

      Then("the event should be deserialised into version 8")
      event.forename should be("joe")
      event.surname should be("bloggs")
      event.active should be(true)
    }

    scenario("Deserialise a version 5 event") {
      Given("a version 5 payload")
      val eventVersion5Payload = objectMapper.createObjectNode()
        .put("forename", "joe")
        .put("surname", "bloggs")
        .toString

      When("deserialising the payload")
      val event = payloadMapper.deserialiseEvent(
        eventVersion5Payload,
        "com.dreweaster.jester.infrastructure.driven.eventstore.mapper.json.EventWithComplexMigrationHistoryClassName2",
        5).asInstanceOf[EventWithComplexMigrationHistoryClassName3]

      Then("the event should be deserialised into version 8")
      event.forename should be("joe")
      event.surname should be("bloggs")
      event.active should be(true)
    }

    scenario("Deserialise a version 6 event") {
      Given("a version 6 payload")
      val eventVersion6Payload = objectMapper.createObjectNode()
        .put("forename", "joe")
        .put("surname", "bloggs")
        .put("activated", false)
        .toString

      When("deserialising the payload")
      val event = payloadMapper.deserialiseEvent(
        eventVersion6Payload,
        "com.dreweaster.jester.infrastructure.driven.eventstore.mapper.json.EventWithComplexMigrationHistoryClassName2",
        6).asInstanceOf[EventWithComplexMigrationHistoryClassName3]

      Then("the event should be deserialised into version 8")
      event.forename should be("joe")
      event.surname should be("bloggs")
      event.active should be(false)
    }

    scenario("Deserialise a version 7 event") {
      Given("a version 7 payload")
      val eventVersion7Payload = objectMapper.createObjectNode()
        .put("forename", "joe")
        .put("surname", "bloggs")
        .put("activated", false)
        .toString

      When("deserialising the payload")
      val event = payloadMapper.deserialiseEvent(
        eventVersion7Payload,
        "com.dreweaster.ddd.jester.infrastructure.driven.eventstore.mapper.json.EventWithComplexMigrationHistoryClassName3",
        7).asInstanceOf[EventWithComplexMigrationHistoryClassName3]

      Then("the event should be deserialised into version 8")
      event.forename should be("joe")
      event.surname should be("bloggs")
      event.active should be(false)
    }

    scenario("Deserialise a version 8 event") {
      Given("a version 8 payload")
      val eventVersion8Payload = objectMapper.createObjectNode()
        .put("forename", "joe")
        .put("surname", "bloggs")
        .put("active", false)
        .toString

      When("deserialising the payload")
      val event = payloadMapper.deserialiseEvent(
        eventVersion8Payload,
        "com.dreweaster.ddd.jester.infrastructure.driven.eventstore.mapper.json.EventWithComplexMigrationHistoryClassName3",
        8).asInstanceOf[EventWithComplexMigrationHistoryClassName3]

      Then("the event should be deserialised into version 8")
      event.forename should be("joe")
      event.surname should be("bloggs")
      event.active should be(false)
    }

    scenario("Throw a MissingDeserialiserException when no deserialiser is found for an event type and version") {
      When("deserialising using an unknown combination of event type and version")
      Then("should throw MissingDeserialiserException")

      assertThrows[MissingDeserialiserException] {
      payloadMapper.deserialiseEvent(
       "{}",
        "com.dreweaster.jester.infrastructure.driven.eventstore.mapper.json.EventWithComplexMigrationHistoryClassName2",
        7).asInstanceOf[EventWithComplexMigrationHistoryClassName3]
      }
    }
  }

  feature("A JsonPayloadMapper can deserialiseEvent multiple conceptual events") {
    val configurers: javaslang.collection.List[JsonEventMappingConfigurer[_]] = javaslang.collection.List.empty()
      .append(new EventWithComplexMigrationHistoryMappingConfigurer).asInstanceOf[javaslang.collection.List[JsonEventMappingConfigurer[_]]]
      .append(new EventWithNoMigrationHistoryMappingConfigurer)

    val stateSerialisers: javaslang.collection.List[StatePayloadJsonSerialiser[_,_]] = javaslang.collection.List.empty()

    val payloadMapper = new JsonPayloadMapper(objectMapper, configurers, stateSerialisers)

    scenario("Deserialises correctly an event that has no com.dreweaster.ddd.jester.infrastructure.driven.eventstore.com.dreweaster.ddd.jester.infrastructure.driven.eventstore.postgres.db.migration history") {
      Given("a payload for the event")
      val eventPayload = objectMapper.createObjectNode()
        .put("forename", "joe")
        .put("surname", "bloggs")
        .put("active", true)
        .toString

      When("deserialising the payload")
      val event = payloadMapper.deserialiseEvent(
        eventPayload,
        "com.dreweaster.ddd.jester.infrastructure.driven.eventstore.mapper.json.EventWithNoMigrationHistory",
        1).asInstanceOf[EventWithNoMigrationHistory]

      Then("the event should be deserialised correctly")
      event.forename should be("joe")
      event.surname should be("bloggs")
      event.active should be(true)
    }

    scenario("Deserialises correctly the latest version of an event with com.dreweaster.ddd.jester.infrastructure.driven.eventstore.com.dreweaster.ddd.jester.infrastructure.driven.eventstore.postgres.db.migration history") {
      Given("a latest versioned payload for the event")
      val eventPayload = objectMapper.createObjectNode()
        .put("forename", "joe")
        .put("surname", "bloggs")
        .put("active", false)
        .toString

      When("deserialising the payload")
      val event = payloadMapper.deserialiseEvent(
        eventPayload,
        "com.dreweaster.ddd.jester.infrastructure.driven.eventstore.mapper.json.EventWithComplexMigrationHistoryClassName3",
        8).asInstanceOf[EventWithComplexMigrationHistoryClassName3]

      Then("the event should be deserialised correctly")
      event.forename should be("joe")
      event.surname should be("bloggs")
      event.active should be(false)
    }
  }

  feature("A JsonPayloadMapper can serialiseEvent multiple conceptual events") {

    val configurers: javaslang.collection.List[JsonEventMappingConfigurer[_]] = javaslang.collection.List.empty()
      .append(new EventWithComplexMigrationHistoryMappingConfigurer).asInstanceOf[javaslang.collection.List[JsonEventMappingConfigurer[_]]]
      .append(new EventWithNoMigrationHistoryMappingConfigurer)

    val stateSerialisers: javaslang.collection.List[StatePayloadJsonSerialiser[_,_]] = javaslang.collection.List.empty()

    val payloadMapper = new JsonPayloadMapper(objectMapper, configurers, stateSerialisers)

    scenario("Serialises correctly an event that has no com.dreweaster.ddd.jester.infrastructure.driven.eventstore.com.dreweaster.ddd.jester.infrastructure.driven.eventstore.postgres.db.migration history") {
      When("serialising an event with no com.dreweaster.ddd.jester.infrastructure.driven.eventstore.com.dreweaster.ddd.jester.infrastructure.driven.eventstore.postgres.db.migration history")
      val result = payloadMapper.serialiseEvent(new EventWithNoMigrationHistory("joe", "bloggs", true))

      Then("the event payload should be serialised correctly in JSON")
      val serialisedPayload = result.payload()
      val payloadAsJson = objectMapper.readTree(serialisedPayload)
      payloadAsJson.get("forename").asText() should be("joe")
      payloadAsJson.get("surname").asText() should be("bloggs")
      payloadAsJson.get("active").asBoolean should be(true)

      And("the correct version should be generated")
      result.version().get() should be(1)
    }

    scenario("Serialises correctly an event with com.dreweaster.ddd.jester.infrastructure.driven.eventstore.com.dreweaster.ddd.jester.infrastructure.driven.eventstore.postgres.db.migration history") {
      When("serialising an event with com.dreweaster.ddd.jester.infrastructure.driven.eventstore.com.dreweaster.ddd.jester.infrastructure.driven.eventstore.postgres.db.migration history")
      val result = payloadMapper.serialiseEvent(new EventWithComplexMigrationHistoryClassName3("joe", "bloggs", true))

      Then("the event payload should be serialised correctly in JSON")
      val serialisedPayload = result.payload()
      val payloadAsJson = objectMapper.readTree(serialisedPayload)
      payloadAsJson.get("forename").asText() should be("joe")
      payloadAsJson.get("surname").asText() should be("bloggs")
      payloadAsJson.get("active").asBoolean should be(true)

      And("the correct version should be generated")
      result.version().get() should be(8)
    }
  }

  feature("A JsonPayloadMapper rejects competing mapping configurers") {
    // TODO: Complete scenarios for this feature
  }

  feature("A JsonPayloadMapper rejects incorrectly configured mapping configurers") {

    scenario("Rejects a null serialiseEvent function") {
      Given("a configurer declaring a null serialiseEvent function")
      val configurer: JsonEventMappingConfigurer[_] = new JsonEventMappingConfigurer[DummyEvent] {
        override def configure(configurationFactory: JsonEventMappingConfigurationFactory[DummyEvent]): Unit = {
          configurationFactory.create("dummy").mappingFunctions(null, new javaslang.Function1[JsonNode, DummyEvent] {
            override def apply(t1: JsonNode): DummyEvent = new DummyEvent()
          })
        }
      }

      val configurers: javaslang.collection.List[JsonEventMappingConfigurer[_]] = javaslang.collection.List.empty().append(configurer)

      val stateSerialisers: javaslang.collection.List[StatePayloadJsonSerialiser[_,_]] = javaslang.collection.List.empty()

      When("creating a JsonPayloadMapper using that configurer")
      val thrown = the[InvalidMappingConfigurationException] thrownBy new JsonPayloadMapper(objectMapper, configurers, stateSerialisers)

      Then("the null serialiseEvent function should be rejected")
      thrown.configurationError() should be(SERIALISE_FUNCTION)
    }

    scenario("Rejects a null deserialiseEvent function") {
      Given("a configurer declaring a null deserialiseEvent function")
      val configurer: JsonEventMappingConfigurer[_] = new JsonEventMappingConfigurer[DummyEvent] {
        override def configure(configurationFactory: JsonEventMappingConfigurationFactory[DummyEvent]): Unit = {
          configurationFactory.create("dummy").mappingFunctions(new javaslang.Function2[DummyEvent, ObjectNode, JsonNode] {
            override def apply(t1: DummyEvent, t2: ObjectNode): JsonNode = t2
          }, null)
        }
      }

      val configurers: javaslang.collection.List[JsonEventMappingConfigurer[_]] = javaslang.collection.List.empty().append(configurer)

      val stateSerialisers: javaslang.collection.List[StatePayloadJsonSerialiser[_,_]] = javaslang.collection.List.empty()

      When("creating a JsonPayloadMapper using that configurer")
      val thrown = the[InvalidMappingConfigurationException] thrownBy new JsonPayloadMapper(objectMapper, configurers, stateSerialisers)

      Then("the null deserialiseEvent function should be rejected")
      thrown.configurationError() should be(DESERIALISE_FUNCTION)
    }

    scenario("Rejects a null initial class name") {
      Given("a configurer declaring a null initial class name")
      val configurer: JsonEventMappingConfigurer[_] = new JsonEventMappingConfigurer[DummyEvent] {
        override def configure(configurationFactory: JsonEventMappingConfigurationFactory[DummyEvent]): Unit = {
          configurationFactory.create(null)
        }
      }

      val configurers: javaslang.collection.List[JsonEventMappingConfigurer[_]] = javaslang.collection.List.empty().append(configurer)

      val stateSerialisers: javaslang.collection.List[StatePayloadJsonSerialiser[_,_]] = javaslang.collection.List.empty()

      When("creating a JsonPayloadMapper using that configurer")
      val thrown = the[InvalidMappingConfigurationException] thrownBy new JsonPayloadMapper(objectMapper, configurers, stateSerialisers)

      Then("the null initial class name should be rejected")
      thrown.configurationError() should be(INITIAL_CLASS_NAME)
    }

    scenario("Rejects a null com.dreweaster.ddd.jester.infrastructure.driven.eventstore.com.dreweaster.ddd.jester.infrastructure.driven.eventstore.postgres.db.migration function") {
      Given("a configurer declaring a null com.dreweaster.ddd.jester.infrastructure.driven.eventstore.com.dreweaster.ddd.jester.infrastructure.driven.eventstore.postgres.db.migration function")
      val configurer: JsonEventMappingConfigurer[_] = new JsonEventMappingConfigurer[DummyEvent] {
        override def configure(configurationFactory: JsonEventMappingConfigurationFactory[DummyEvent]): Unit = {
          configurationFactory
            .create("dummy")
            .migrateFormat(null)
            .mappingFunctions(
              new javaslang.Function2[DummyEvent, ObjectNode, JsonNode] {
                override def apply(t1: DummyEvent, t2: ObjectNode): JsonNode = t2
              },
              new javaslang.Function1[JsonNode, DummyEvent] {
                override def apply(t1: JsonNode): DummyEvent = new DummyEvent()
              }
            )
        }
      }

      val configurers: javaslang.collection.List[JsonEventMappingConfigurer[_]] = javaslang.collection.List.empty().append(configurer)

      val stateSerialisers: javaslang.collection.List[StatePayloadJsonSerialiser[_,_]] = javaslang.collection.List.empty()

      When("creating a JsonPayloadMapper using that configurer")
      val thrown = the[InvalidMappingConfigurationException] thrownBy new JsonPayloadMapper(objectMapper, configurers, stateSerialisers)

      Then("the null null com.dreweaster.ddd.jester.infrastructure.driven.eventstore.com.dreweaster.ddd.jester.infrastructure.driven.eventstore.postgres.db.migration function should be rejected")
      thrown.configurationError() should be(MIGRATION_FUNCTION)
    }

    scenario("Rejects a null com.dreweaster.ddd.jester.infrastructure.driven.eventstore.com.dreweaster.ddd.jester.infrastructure.driven.eventstore.postgres.db.migration class name") {
      Given("a configurer declaring a null com.dreweaster.ddd.jester.infrastructure.driven.eventstore.com.dreweaster.ddd.jester.infrastructure.driven.eventstore.postgres.db.migration class name")
      val configurer: JsonEventMappingConfigurer[_] = new JsonEventMappingConfigurer[DummyEvent] {
        override def configure(configurationFactory: JsonEventMappingConfigurationFactory[DummyEvent]): Unit = {
          configurationFactory
            .create("dummy")
            .migrateClassName(null)
            .mappingFunctions(
              new javaslang.Function2[DummyEvent, ObjectNode, JsonNode] {
                override def apply(t1: DummyEvent, t2: ObjectNode): JsonNode = t2
              },
              new javaslang.Function1[JsonNode, DummyEvent] {
                override def apply(t1: JsonNode): DummyEvent = new DummyEvent()
              }
            )
        }
      }

      val configurers: javaslang.collection.List[JsonEventMappingConfigurer[_]] = javaslang.collection.List.empty().append(configurer)

      val stateSerialisers: javaslang.collection.List[StatePayloadJsonSerialiser[_,_]] = javaslang.collection.List.empty()

      When("creating a JsonPayloadMapper using that configurer")
      val thrown = the[InvalidMappingConfigurationException] thrownBy new JsonPayloadMapper(objectMapper, configurers, stateSerialisers)

      Then("the null null com.dreweaster.ddd.jester.infrastructure.driven.eventstore.com.dreweaster.ddd.jester.infrastructure.driven.eventstore.postgres.db.migration class name should be rejected")
      thrown.configurationError() should be(MIGRATION_CLASS_NAME)
    }
  }
}

class DummyEvent(data: String = "") extends DomainEvent {
  override def tag(): DomainEventTag = DomainEventTag.of("dummy-event")
}

class EventWithComplexMigrationHistoryClassName3(val forename: String, val surname: String, val active: Boolean) extends DomainEvent {
  override def tag(): DomainEventTag = DomainEventTag.of("dummy-event")
}

class EventWithNoMigrationHistory(val forename: String, val surname: String, val active: Boolean) extends DomainEvent {
  override def tag(): DomainEventTag = DomainEventTag.of("dummy-event")
}

class EventWithComplexMigrationHistoryMappingConfigurer extends JsonEventMappingConfigurer[EventWithComplexMigrationHistoryClassName3] {

  def configure(configurationFactory: JsonEventMappingConfigurationFactory[EventWithComplexMigrationHistoryClassName3]) {
    configurationFactory.create("com.dreweaster.jester.infrastructure.driven.eventstore.mapper.json.EventWithComplexMigrationHistoryClassName1")
      .migrateFormat(migrateVersion1ToVersion2)
      .migrateFormat(migrateVersion2ToVersion3)
      .migrateFormat(migrateVersion3ToVersion4)
      .migrateClassName("com.dreweaster.jester.infrastructure.driven.eventstore.mapper.json.EventWithComplexMigrationHistoryClassName2")
      .migrateFormat(migrateVersion4ToVersion6)
      .migrateClassName(classOf[EventWithComplexMigrationHistoryClassName3].getName)
      .migrateFormat(migrateVersion6ToVersion8)
      .mappingFunctions(serialise, deserialise)
  }

  val serialise: javaslang.Function2[EventWithComplexMigrationHistoryClassName3, ObjectNode, JsonNode] =
    new javaslang.Function2[EventWithComplexMigrationHistoryClassName3, ObjectNode, JsonNode] {
      override def apply(event: EventWithComplexMigrationHistoryClassName3, root: ObjectNode): JsonNode = {
        root
          .put("forename", event.forename)
          .put("surname", event.surname)
          .put("active", event.active)
      }
    }

  val deserialise: javaslang.Function1[JsonNode, EventWithComplexMigrationHistoryClassName3] = new javaslang.Function1[JsonNode, EventWithComplexMigrationHistoryClassName3] {
    override def apply(root: JsonNode): EventWithComplexMigrationHistoryClassName3 =
      new EventWithComplexMigrationHistoryClassName3(
        forename = root.get("forename").asText,
        surname = root.get("surname").asText,
        active = root.get("active").asBoolean())
  }

  val migrateVersion1ToVersion2: javaslang.Function1[JsonNode, JsonNode] = new javaslang.Function1[JsonNode, JsonNode] {
    override def apply(node: JsonNode): JsonNode = {
      val firstName = node.get("firstName").asText()
      val secondName = node.get("secondName").asText()

      node.asInstanceOf[ObjectNode].removeAll()
        .put("first_name", firstName)
        .put("second_name", secondName)
    }
  }

  val migrateVersion2ToVersion3: javaslang.Function1[JsonNode, JsonNode] = new javaslang.Function1[JsonNode, JsonNode] {
    override def apply(node: JsonNode): JsonNode = {
      val secondName = node.get("second_name").asText()
      node.asInstanceOf[ObjectNode].remove("second_name")
      node.asInstanceOf[ObjectNode].put("last_name", secondName)
    }
  }

  val migrateVersion3ToVersion4: javaslang.Function1[JsonNode, JsonNode] = new javaslang.Function1[JsonNode, JsonNode] {
    override def apply(node: JsonNode): JsonNode = {
      val firstName = node.get("first_name").asText()
      val lastName = node.get("last_name").asText()
      node.asInstanceOf[ObjectNode].removeAll()
        .put("forename", firstName)
        .put("surname", lastName)
    }
  }

  val migrateVersion4ToVersion6: javaslang.Function1[JsonNode, JsonNode] = new javaslang.Function1[JsonNode, JsonNode] {
    override def apply(node: JsonNode): JsonNode = {
      node.asInstanceOf[ObjectNode]
        .put("activated", true)
    }
  }

  val migrateVersion6ToVersion8: javaslang.Function1[JsonNode, JsonNode] = new javaslang.Function1[JsonNode, JsonNode] {
    override def apply(node: JsonNode): JsonNode = {
      val activated = node.get("activated").asBoolean()
      node.asInstanceOf[ObjectNode].remove("activated")
      node.asInstanceOf[ObjectNode].put("active", activated)
    }
  }
}

class EventWithNoMigrationHistoryMappingConfigurer extends JsonEventMappingConfigurer[EventWithNoMigrationHistory] {

  def configure(configurationFactory: JsonEventMappingConfigurationFactory[EventWithNoMigrationHistory]) {
    configurationFactory.create(classOf[EventWithNoMigrationHistory].getName)
      .mappingFunctions(serialise, deserialise)
  }

  val serialise: javaslang.Function2[EventWithNoMigrationHistory, ObjectNode, JsonNode] =
    new javaslang.Function2[EventWithNoMigrationHistory, ObjectNode, JsonNode] {
      override def apply(event: EventWithNoMigrationHistory, root: ObjectNode): JsonNode = {
        root
          .put("forename", event.forename)
          .put("surname", event.surname)
          .put("active", event.active)
      }
    }

  val deserialise: javaslang.Function1[JsonNode, EventWithNoMigrationHistory] = new javaslang.Function1[JsonNode, EventWithNoMigrationHistory] {
    override def apply(root: JsonNode): EventWithNoMigrationHistory =
      new EventWithNoMigrationHistory(
        forename = root.get("forename").asText,
        surname = root.get("surname").asText,
        active = root.get("active").asBoolean())
  }
}
