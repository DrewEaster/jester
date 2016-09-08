package com.dreweaster.jester.domain

import javaslang.concurrent.Future

import com.dreweaster.jester.application.eventstore.EventStore.OptimisticConcurrencyException
import com.dreweaster.jester.application.eventstore.PersistedEvent
import com.dreweaster.jester.application.repository.deduplicating.{CommandDeduplicationStrategy, CommandDeduplicationStrategyBuilder, CommandDeduplicationStrategyFactory}
import com.dreweaster.jester.domain.AggregateRepository.AggregateRoot.{NoHandlerForEvent, NoHandlerForCommand}
import com.dreweaster.jester.domain.AggregateRepository.CommandEnvelope
import com.dreweaster.jester.example.application.repository.CommandDeduplicatingEventsourcedUserRepository
import com.dreweaster.jester.example.domain.aggregates.user.User.AlreadyRegistered
import com.dreweaster.jester.example.domain.aggregates.user.commands.{ChangeUsername, IncrementFailedLoginAttempts, ChangePassword, RegisterUser}
import com.dreweaster.jester.example.domain.aggregates.user.events.{UserLocked, FailedLoginAttemptsIncremented, PasswordChanged, UserRegistered}
import com.dreweaster.jester.infrastructure.MockEventStore
import org.scalatest.{Matchers, BeforeAndAfter, GivenWhenThen, FlatSpec}

/**
  */
class AggregateRootBehaviourTest extends FlatSpec with GivenWhenThen with BeforeAndAfter with Matchers {

  // Dummy Event Store will implicitly ensure that sequence numbers are handled correctly in the repository impl.
  // An OptimisticCurrencyException will be returned if the code passed the incorrect expected sequence number
  // to the event store when saving generated events.
  val eventStore = new MockEventStore()

  val deduplicationStrategyFactory = new SwitchableDeduplicationStrategyFactory

  val userRepository = new CommandDeduplicatingEventsourcedUserRepository(eventStore, deduplicationStrategyFactory)

  before {
    eventStore.clear()
    eventStore.toggleLoadErrorStateOff()
    eventStore.toggleSaveErrorStateOff()
    deduplicationStrategyFactory.toggleDeduplicationOn()
  }

  "An AggregateRoot" should "be createable for the first time" in {
    Given("an aggregate that has not yet been created")
    val user = userRepository.aggregateRootOf(AggregateId.of("some-aggregate-id"))

    When("sending a command to create the aggregate")
    val futureEvents = await(user.handle(
      CommandEnvelope.of(
        CommandId.of("some_command_id"),
        RegisterUser.builder()
          .username("joebloggs")
          .password("password")
          .create())))

    Then("the command result should be successful")
    futureEvents.isSuccess should be(true)

    And("the aggregate should be created")
    futureEvents.get().size() should be(1)
    futureEvents.get().get(0) should be(UserRegistered.builder()
      .username("joebloggs")
      .password("password")
      .create())
  }

  it should "be able to refer to existing state when processing a command" in {
    Given("an aggregate that has been created")
    val user = userRepository.aggregateRootOf(AggregateId.of("some-aggregate-id"))
    await(user.handle(
      CommandEnvelope.of(
        CommandId.of("some_command_id"),
        RegisterUser.builder()
          .username("joebloggs")
          .password("password")
          .create())))

    When("sending a command that queries existing state")
    val futureEvents = await(user.handle(
      CommandEnvelope.of(
        CommandId.of("some_other_command_id"),
        ChangePassword.builder()
          .password("changedPassword")
          .create())))

    Then("the command result should be successful")
    futureEvents.isSuccess should be(true)

    And("the emitted event should correctly refer to some existing state")
    futureEvents.get().size() should be(1)
    futureEvents.get().get(0) should be(PasswordChanged.builder()
      .password("changedPassword")
      .oldPassword("password")
      .create())
  }

  it should "support emitting multiple events for a single input command" in {
    Given("An aggregate thats handled some commands")
    val user = userRepository.aggregateRootOf(AggregateId.of("some-aggregate-id"))
    await(user.handle(
      CommandEnvelope.of(
        CommandId.of("command_id_1"),
        RegisterUser.builder()
          .username("joebloggs")
          .password("password")
          .create())))

    await(user.handle(CommandEnvelope.of(CommandId.of("command_id_2"), IncrementFailedLoginAttempts.of())))
    await(user.handle(CommandEnvelope.of(CommandId.of("command_id_3"), IncrementFailedLoginAttempts.of())))
    await(user.handle(CommandEnvelope.of(CommandId.of("command_id_4"), IncrementFailedLoginAttempts.of())))

    When("sending a command that should lead to multiple events being emitted")
    val futureEvents = await(user.handle(CommandEnvelope.of(CommandId.of("command_id_5"), IncrementFailedLoginAttempts.of())))

    Then("multiple events should be emitted as expected")
    futureEvents.isSuccess should be(true)
    futureEvents.get().size() should be(2)
    futureEvents.get().get(0) should be(FailedLoginAttemptsIncremented.of())
    futureEvents.get().get(1) should be(UserLocked.of())
  }

  it should "ignore a command with a previously handled command id and of the same command type" in {
    Given("an aggregate created with a deterministic command id")
    val user = userRepository.aggregateRootOf(AggregateId.of("some-aggregate-id"))
    await(user.handle(
      CommandEnvelope.of(
        CommandId.of("some_command_id"),
        RegisterUser.builder()
          .username("joebloggs")
          .password("password")
          .create())))

    When("sending the same command id again using a command of the type")
    val futureEvents = await(user.handle(
      CommandEnvelope.of(
        CommandId.of("some_command_id"),
        RegisterUser.builder()
          .username("joebloggs")
          .password("password")
          .create())))

    Then("the command result should be successful")
    futureEvents.isSuccess should be(true)

    And("no events should be generated")
    futureEvents.get().size() should be(0)
  }

  it should "ignore a command with a previously handled command id and of a different command type" in {
    Given("an aggregate created with a deterministic command id")
    val user = userRepository.aggregateRootOf(AggregateId.of("some-aggregate-id"))
    await(user.handle(
      CommandEnvelope.of(
        CommandId.of("some_command_id"),
        RegisterUser.builder()
          .username("joebloggs")
          .password("password")
          .create())))

    When("sending the same command id again using a command of a different type")
    val futureEvents = await(user.handle(
      CommandEnvelope.of(
        CommandId.of("some_command_id"),
        ChangePassword.builder()
          .password("newPassword")
          .create())))

    Then("the command result should be successful")
    futureEvents.isSuccess should be(true)

    And("no events should be generated")
    futureEvents.get().size() should be(0)
  }

  it should "propagate error when a command is explicitly rejected in its current behaviour" in {
    Given("an aggregate")
    val user = userRepository.aggregateRootOf(AggregateId.of("some-aggregate-id"))
    await(user.handle(
      CommandEnvelope.of(
        CommandId.of("some_command_id"),
        RegisterUser.builder()
          .username("joebloggs")
          .password("password")
          .create())))

    When("sending a command that will be rejected in its current behaviour")
    val futureEvents = await(user.handle(
      CommandEnvelope.of(
        CommandId.of("some_other_command_id"),
        RegisterUser.builder()
          .username("joebloggs")
          .password("password")
          .create())))

    Then("the command should fail")
    futureEvents.isSuccess should be(false)

    And("the correct command error should be returned")
    futureEvents.getCause.get shouldBe an[AlreadyRegistered]
  }

  it should "generate an error when a command is not explicitly handled in its current behaviour" in {
    Given("an aggregate")
    val user = userRepository.aggregateRootOf(AggregateId.of("some-aggregate-id"))

    When("sending a command that isn't explicitly handled in its current behaviour")
    val futureEvents = await(user.handle(
      CommandEnvelope.of(
        CommandId.of("some_command_id"),
        ChangePassword.builder()
          .password("newPassword")
          .create())))

    Then("the command should fail")
    futureEvents.isSuccess should be(false)

    And("the unhandled command error should be returned")
    futureEvents.getCause.get shouldBe an[NoHandlerForCommand]
  }

  it should "generate an error during recovery when encountering an event not supported in its current behaviour" in {
    Given("an aggregate with a missing event handler")
    val user = userRepository.aggregateRootOf(AggregateId.of("some-aggregate-id"))
    await(user.handle(
      CommandEnvelope.of(
        CommandId.of("some_command_id"),
        RegisterUser.builder()
          .username("joebloggs")
          .password("password")
          .create())))

    And("the unhandled event has been persisted")
    await(user.handle(
      CommandEnvelope.of(
        CommandId.of("some_other_command_id"),
        ChangeUsername.builder()
          .username("johnsmith")
          .create())))

    When("sending a command")
    val futureEvents = await(user.handle(
      CommandEnvelope.of(
        CommandId.of("yet_another_command_id"),
        ChangePassword.builder()
          .password("newPassword")
          .create())))

    Then("the command should fail")
    futureEvents.isSuccess should be(false)

    And("the unhandled command error should be returned")
    futureEvents.getCause.get shouldBe an[NoHandlerForEvent]
  }

  it should "propagate error when event store fails to load events" in {
    Given("the event store can't load events")
    eventStore.toggleLoadErrorStateOn()

    And("an aggregate")
    val user = userRepository.aggregateRootOf(AggregateId.of("some-aggregate-id"))

    When("sending a command")
    val futureEvents = await(user.handle(
      CommandEnvelope.of(
        CommandId.of("some_command_id"),
        RegisterUser.builder()
          .username("joebloggs")
          .password("password")
          .create())))

    Then("the command should fail")
    futureEvents.isSuccess should be(false)

    And("the event store's error should be returned")
    futureEvents.getCause.get shouldBe an[OptimisticConcurrencyException]
  }

  it should "propagate error when event store fails to save generated events" in {
    Given("the event store can't load events")
    eventStore.toggleSaveErrorStateOn()

    And("an aggregate")
    val user = userRepository.aggregateRootOf(AggregateId.of("some-aggregate-id"))

    When("sending a command")
    val futureEvents = await(user.handle(
      CommandEnvelope.of(
        CommandId.of("some_command_id"),
        RegisterUser.builder()
          .username("joebloggs")
          .password("password")
          .create())))

    Then("the command should fail")
    futureEvents.isSuccess should be(false)

    And("the event store's error should be returned")
    futureEvents.getCause.get shouldBe an[IllegalStateException]
  }

  it should "process a duplicate command if the deduplication strategy says it's ok to process it" in {
    Given("the deduplication strategy is not configured to perform deduplication")
    deduplicationStrategyFactory.toggleDeduplicationOff()

    And("an aggregate")
    val user = userRepository.aggregateRootOf(AggregateId.of("some-aggregate-id"))
    await(user.handle(
      CommandEnvelope.of(
        CommandId.of("some_command_id"),
        RegisterUser.builder()
          .username("joebloggs")
          .password("password")
          .create())))

    When("sending the two commands with the same command id")
    await(user.handle(CommandEnvelope.of(CommandId.of("command_id"), ChangePassword.builder().password("newPassword").create())))
    val futureEvents = await(user.handle(
      CommandEnvelope.of(
        CommandId.of("command_id"),
        ChangePassword.builder()
          .password("anotherNewPassword")
          .create())))

    Then("the second command will not be deduplicated")
    futureEvents.isSuccess should be(true)
    futureEvents.get().size() should be(1)
    futureEvents.get().get(0) should be(PasswordChanged.builder()
      .password("anotherNewPassword")
      .oldPassword("newPassword").create())
  }

  private def await[T](future: Future[T]) = {
    future.await()
    future
  }

  final class SwitchableDeduplicationStrategyFactory extends CommandDeduplicationStrategyFactory {

    private var deduplicationEnabled = true

    def toggleDeduplicationOn() {
      deduplicationEnabled = true
    }

    def toggleDeduplicationOff() {
      deduplicationEnabled = false
    }

    def newBuilder: CommandDeduplicationStrategyBuilder = {
      new CommandDeduplicationStrategyBuilder() {
        private var causationIds: Set[CausationId] = Set()

        def addEvent(domainEvent: PersistedEvent[_, _]): CommandDeduplicationStrategyBuilder = {
          causationIds = causationIds + domainEvent.causationId()
          this
        }

        def build: CommandDeduplicationStrategy = {
          new CommandDeduplicationStrategy {
            override def isDuplicate(commandId: CommandId) =
              if (deduplicationEnabled) causationIds.contains(CausationId.of(commandId.get())) else false
          }
        }
      }
    }
  }

}
