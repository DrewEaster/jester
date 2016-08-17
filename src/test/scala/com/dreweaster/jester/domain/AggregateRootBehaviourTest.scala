package com.dreweaster.jester.domain

import javaslang.concurrent.Future

import com.dreweaster.jester.application.{AlwaysDeduplicateStrategyFactory, CommandDeduplicatingEventsourcedUserRepository}
import com.dreweaster.jester.domain.AggregateRepository.AggregateRoot.{NoHandlerForEvent, NoHandlerForCommand}
import com.dreweaster.jester.domain.User.AlreadyRegistered
import com.dreweaster.jester.infrastructure.eventstore.driven.dummy.DummyEventStore
import org.scalatest.{Matchers, BeforeAndAfter, GivenWhenThen, FlatSpec}

/**
  */
class AggregateRootBehaviourTest extends FlatSpec with GivenWhenThen with BeforeAndAfter with Matchers {

  // Dummy Event Store will implicitly ensure that sequence numbers are handled correctly in the repository impl.
  // An OptimisticCurrencyException will be returned if the code passed the incorrect expected sequence number
  // to the event store when saving generated events.
  val eventStore = new DummyEventStore()

  val userRepository = new CommandDeduplicatingEventsourcedUserRepository(eventStore, new AlwaysDeduplicateStrategyFactory)

  before {
    eventStore.reset()
    eventStore.toggleLoadErrorStateOff()
    eventStore.toggleSaveErrorStateOff()
  }

  "An AggregateRoot" should "be createable for the first time" in {
    Given("an aggregate that has not yet been created")
    val user = userRepository.aggregateRootOf(AggregateId.of("some-aggregate-id"))

    When("sending a command to create the aggregate")
    val futureEvents = await(user.handle(CommandId.of("some_command_id"), RegisterUser.of("joebloggs", "password")))

    Then("the command result should be successful")
    futureEvents.isSuccess should be(true)

    And("the aggregate should be created")
    futureEvents.get().size() should be(1)
    futureEvents.get().get(0) should be(UserRegistered.of("joebloggs", "password"))
  }

  it should "be able to refer to existing state when processing a command" in {
    Given("an aggregate that has been created")
    val user = userRepository.aggregateRootOf(AggregateId.of("some-aggregate-id"))
    await(user.handle(CommandId.of("some_command_id"), RegisterUser.of("joebloggs", "password")))

    When("sending a command that queries existing state")
    val futureEvents = await(user.handle(CommandId.of("some_other_command_id"), ChangePassword.of("changedPassword")))

    Then("the command result should be successful")
    futureEvents.isSuccess should be(true)

    And("the emitted event should correctly refer to some existing state")
    futureEvents.get().size() should be(1)
    futureEvents.get().get(0) should be(PasswordChanged.of("changedPassword", "password"))
  }

  it should "ignore a command with a previously handled command id and of the same command type" in {
    Given("an aggregate created with a deterministic command id")
    val user = userRepository.aggregateRootOf(AggregateId.of("some-aggregate-id"))
    await(user.handle(CommandId.of("some_command_id"), RegisterUser.of("joebloggs", "password")))

    When("sending the same command id again using a command of the type")
    val futureEvents = await(user.handle(CommandId.of("some_command_id"), RegisterUser.of("joebloggs", "password")))

    Then("the command result should be successful")
    futureEvents.isSuccess should be(true)

    And("no events should be generated")
    futureEvents.get().size() should be(0)
  }

  it should "ignore a command with a previously handled command id and of a different command type" in {
    Given("an aggregate created with a deterministic command id")
    val user = userRepository.aggregateRootOf(AggregateId.of("some-aggregate-id"))
    await(user.handle(CommandId.of("some_command_id"), RegisterUser.of("joebloggs", "password")))

    When("sending the same command id again using a command of a different type")
    val futureEvents = await(user.handle(CommandId.of("some_command_id"), ChangePassword.of("newPassword")))
    futureEvents.await()

    Then("the command result should be successful")
    futureEvents.isSuccess should be(true)

    And("no events should be generated")
    futureEvents.get().size() should be(0)
  }

  it should "propagate error when a command is explicitly rejected in its current behaviour" in {
    Given("an aggregate")
    val user = userRepository.aggregateRootOf(AggregateId.of("some-aggregate-id"))
    await(user.handle(CommandId.of("some_command_id"), RegisterUser.of("joebloggs", "password")))

    When("sending a command that will be rejected in its current behaviour")
    val futureEvents = await(user.handle(CommandId.of("some_other_command_id"), RegisterUser.of("joebloggs", "password")))

    Then("the command should fail")
    futureEvents.isSuccess should be(false)

    And("the correct command error should be returned")
    futureEvents.getCause.get shouldBe an[AlreadyRegistered]
  }

  it should "generate an error when a command is not explicitly handled in its current behaviour" in {
    Given("an aggregate")
    val user = userRepository.aggregateRootOf(AggregateId.of("some-aggregate-id"))

    When("sending a command that isn't explicitly handled in its current behaviour")
    val futureEvents = await(user.handle(CommandId.of("some_command_id"), ChangePassword.of("newPassword")))

    Then("the command should fail")
    futureEvents.isSuccess should be(false)

    And("the unhandled command error should be returned")
    futureEvents.getCause.get shouldBe an[NoHandlerForCommand]
  }

  it should "generate an error during recovery when encountering an event not supported in its current behaviour" in {
    Given("an aggregate with a missing event handler")
    val user = userRepository.aggregateRootOf(AggregateId.of("some-aggregate-id"))

    And("where the unhandled event has been persisted")
    await(user.handle(CommandId.of("some_command_id"), RegisterUser.of("joebloggs", "password")))
    await(user.handle(CommandId.of("some_other_command_id"), ChangeUsername.of("johnsmith")))

    When("sending a command")
    val futureEvents = await(user.handle(CommandId.of("yet_another_command_id"), ChangePassword.of("newPassword")))

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
    val futureEvents = await(user.handle(CommandId.of("some_command_id"), RegisterUser.of("joebloggs", "password")))

    Then("the command should fail")
    futureEvents.isSuccess should be(false)

    And("the event store's error should be returned")
    futureEvents.getCause.get shouldBe an[IllegalStateException]
  }

  it should "propagate error when event store fails to save generated events" in {
    Given("the event store can't load events")
    eventStore.toggleSaveErrorStateOn()

    And("an aggregate")
    val user = userRepository.aggregateRootOf(AggregateId.of("some-aggregate-id"))

    When("sending a command")
    val futureEvents = await(user.handle(CommandId.of("some_command_id"), RegisterUser.of("joebloggs", "password")))

    Then("the command should fail")
    futureEvents.isSuccess should be(false)

    And("the event store's error should be returned")
    futureEvents.getCause.get shouldBe an[IllegalStateException]
  }

  it should "process a duplicate command if the deduplication strategy says it's ok to process it" in {

  }

  private def await[T](future: Future[T]) = {
    future.await()
    future
  }
}
