package com.dreweaster.ddd.jester.behaviour

import io.vavr.concurrent.Future
import io.vavr.control.Option
import com.dreweaster.ddd.jester.application.eventstore.PersistedEvent
import com.dreweaster.ddd.jester.application.repository.{CommandDeduplicationStrategy, CommandDeduplicationStrategyBuilder, CommandDeduplicationStrategyFactory}
import com.dreweaster.ddd.jester.application.repository.monitoring.{AggregateRepositoryReporter, CommandHandlingProbe}
import com.dreweaster.ddd.jester.domain.AggregateRepository.AggregateRoot.{NoHandlerForCommand, NoHandlerForEvent}
import com.dreweaster.ddd.jester.domain._
import com.dreweaster.ddd.jester.domain.AggregateRepository.{CommandEnvelope, ConcurrentModificationResult, RejectionResult, SuccessResult}
import com.dreweaster.ddd.jester.example.application.repository.CommandDeduplicatingEventsourcedUserRepository
import com.dreweaster.ddd.jester.example.domain.aggregates.user.User.AlreadyRegistered
import com.dreweaster.ddd.jester.infrastructure.driven.eventstore.MockEventStore
import org.scalatest.{BeforeAndAfter, FlatSpec, GivenWhenThen, Matchers}
import com.dreweaster.ddd.jester.example.domain.aggregates.user.events._
import com.dreweaster.ddd.jester.example.domain.aggregates.user.commands._
import com.dreweaster.ddd.jester.example.domain.aggregates.user._
import io.vavr.collection

/**
  * TODO: Add tests to show that reporting probes are called correctly in all common scenarios
  */
class AggregateRootBehaviourTest extends FlatSpec with GivenWhenThen with BeforeAndAfter with Matchers {

  // Dummy Event Store will implicitly ensure that sequence numbers are handled correctly in the repository impl.
  // An OptimisticCurrencyException will be returned if the code passed the incorrect expected sequence number
  // to the event store when saving generated events.
  val eventStore = new MockEventStore()

  val deduplicationStrategyFactory = new SwitchableDeduplicationStrategyFactory

  val userRepository = new CommandDeduplicatingEventsourcedUserRepository(eventStore, deduplicationStrategyFactory)

  val reporter = new Reporter

  // userRepository.addReporter(reporter)

  before {
    eventStore.clear()
    eventStore.toggleLoadErrorStateOff()
    eventStore.toggleSaveErrorStateOff()
    eventStore.toggleOffOptimisticConcurrencyExceptionOnSave()
    deduplicationStrategyFactory.toggleDeduplicationOn()
  }

  "An AggregateRoot" should "be createable for the first time" in {
    Given("an aggregate that has not yet been created")
    val user = userRepository.aggregateRootOf(AggregateId.of("some-aggregate-id"))

    When("sending a command to create the aggregate")
    val result = await(user.handle(
      CommandEnvelope.of(
        CommandId.of("some_command_id"),
        RegisterUser.builder()
          .username("joebloggs")
          .password("password")
          .create())))

    Then("the command result should be successful")
    val successResult = result.asInstanceOf[Future[SuccessResult[_,_]]]

    And("the aggregate should be created")
    successResult.get().generatedEvents().size() should be(1)
    successResult.get().generatedEvents().get(0) should be(UserRegistered.builder()
      .username("joebloggs")
      .password("password")
      .create())
  }

  it should "return empty option when fetching state prior to its creation" in {
    Given("an aggregate that has not yet been created")
    val user = userRepository.aggregateRootOf(AggregateId.of("some-aggregate-id"))

    When("trying to fetch state")
    val state = await(user.state())

    Then("the state should be an empty option")
    state.get() should be (Option.none())
  }

  it should "return the initial state following handling its creation command" in {
    Given("an aggregate that has not yet been created")
    val user = userRepository.aggregateRootOf(AggregateId.of("some-aggregate-id"))

    When("sending a command to create the aggregate")
    await(user.handle(
      CommandEnvelope.of(
        CommandId.of("some_command_id"),
        RegisterUser.builder()
          .username("joebloggs")
          .password("password")
          .create())))

    And("then fetching the current state")
    val state = await(user.state())

    Then("the state should be defined")
    state.get().isDefined should be(true)

    And("the state should be correct")
    state.get().get() should be(UserState.builder()
      .username("joebloggs")
      .password("password")
      .failedLoginAttempts(0)
      .create())
  }

  it should "return current state following processing of multiple commands" in  {
    Given("An aggregate that's handled some commands")
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

    And("then fetching the current state")
    val state = await(user.state())

    Then("the state should be defined")
    state.get().isDefined should be(true)

    And("the state should be correct")
    state.get().get() should be(UserState.builder()
      .username("joebloggs")
      .password("password")
      .failedLoginAttempts(3)
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
    val result = await(user.handle(
      CommandEnvelope.of(
        CommandId.of("some_other_command_id"),
        ChangePassword.builder()
          .password("changedPassword")
          .create())))

    Then("the command result should be successful")
    val successResult = result.asInstanceOf[Future[SuccessResult[_,_]]]

    And("the emitted event should correctly refer to some existing state")
    successResult.get().generatedEvents().size() should be(1)
    successResult.get().generatedEvents().get(0) should be(PasswordChanged.builder()
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
    val result = await(user.handle(CommandEnvelope.of(CommandId.of("command_id_5"), IncrementFailedLoginAttempts.of())))

    Then("the command result should be successful")
    val successResult = result.asInstanceOf[Future[SuccessResult[_,_]]]

    And("multiple events should be emitted as expected")
    successResult.get().generatedEvents().size() should be(2)
    successResult.get().generatedEvents().get(0) should be(FailedLoginAttemptsIncremented.of())
    successResult.get().generatedEvents().get(1) should be(UserLocked.of())
  }

  it should "deduplicate a command with a previously handled command id and of the same command type" in {
    Given("An aggregate that's handled some commands")
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
    val firstResult = await(user.handle(CommandEnvelope.of(CommandId.of("command_id_5"), IncrementFailedLoginAttempts.of())))
    val eventsGeneratedWhenCommandFirstHandled = firstResult.asInstanceOf[Future[SuccessResult[_,_]]].get().generatedEvents()

    When("sending the same command id again using a command of the same type")
    val secondResult = await(user.handle(CommandEnvelope.of(CommandId.of("command_id_5"), IncrementFailedLoginAttempts.of())))

    Then("the command result should be successful")
    val successResult = secondResult.asInstanceOf[Future[SuccessResult[_,_]]]

    And("the result should be flagged as deduplicated")
    successResult.get().wasDeduplicated() should be (true)

    And("the same events as generated when the command was first handled should be returned again")
    successResult.get().generatedEvents() should be (eventsGeneratedWhenCommandFirstHandled)
  }

//  it should "ignore a command with a previously handled command id and of a different command type" in {
//    Given("an aggregate created with a deterministic command id")
//    val user = userRepository.aggregateRootOf(AggregateId.of("some-aggregate-id"))
//    await(user.handle(
//      CommandEnvelope.of(
//        CommandId.of("some_command_id"),
//        RegisterUser.builder()
//          .username("joebloggs")
//          .password("password")
//          .create())))
//
//    When("sending the same command id again using a command of a different type")
//    val futureEvents = await(user.handle(
//      CommandEnvelope.of(
//        CommandId.of("some_command_id"),
//        ChangePassword.builder()
//          .password("newPassword")
//          .create())))
//
//    Then("the command result should be successful")
//    futureEvents.isSuccess should be(true)
//
//    And("no events should be generated")
//    futureEvents.get().size() should be(0)
//  }

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
    val result = await(user.handle(
      CommandEnvelope.of(
        CommandId.of("some_other_command_id"),
        RegisterUser.builder()
          .username("joebloggs")
          .password("password")
          .create())))

    Then("the command should be rejected")
    val rejectionResult = result.asInstanceOf[Future[RejectionResult[_,_]]]

    And("the correct command error should be returned")
    rejectionResult.get().error() shouldBe an[AlreadyRegistered]
  }

  it should "generate an error when a command is not explicitly handled in its current behaviour" in {
    Given("an aggregate")
    val user = userRepository.aggregateRootOf(AggregateId.of("some-aggregate-id"))

    When("sending a command that isn't explicitly handled in its current behaviour")
    val result = await(user.handle(
      CommandEnvelope.of(
        CommandId.of("some_command_id"),
        ChangePassword.builder()
          .password("newPassword")
          .create())))

    Then("the command should fail")
    result.isSuccess should be(false)

    And("the unhandled command error should be returned")
    result.getCause.get shouldBe an[NoHandlerForCommand]
  }

  it should "generate an error when handling a command for which there is no corresponding event supported in its current behaviour" in {
    Given("an aggregate with a missing event handler")
    val user = userRepository.aggregateRootOf(AggregateId.of("some-aggregate-id"))
    await(user.handle(
      CommandEnvelope.of(
        CommandId.of("some_command_id"),
        RegisterUser.builder()
          .username("joebloggs")
          .password("password")
          .create())))

    When("Sending a command that has no corresponding event handler")
    val result = await(user.handle(
      CommandEnvelope.of(
        CommandId.of("some_other_command_id"),
        ChangeUsername.builder()
          .username("johnsmith")
          .create())))

    Then("the command should fail")
    result.isSuccess should be(false)

    And("the no handler for event error should be returned")
    result.getCause.get shouldBe an[NoHandlerForEvent]
  }

  it should "propagate error if event store fails to load events when handling a command" in {
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
    futureEvents.getCause.get shouldBe an[IllegalStateException]
  }

  it should "propagate error if event store fails to load events when fetching state" in {
    Given("the event store can't load events")
    eventStore.toggleLoadErrorStateOn()

    And("an aggregate")
    val user = userRepository.aggregateRootOf(AggregateId.of("some-aggregate-id"))

    When("fetching current state")
    val state = await(user.state())

    Then("the operation should fail")
    state.isSuccess should be(false)

    And("the event store's error should be returned")
    state.getCause.get shouldBe an[IllegalStateException]
  }

  it should "return a ConcurrentModification result when saving to event store generates an OptimisticConcurrencyException" in {
    Given("an aggregate that's modified by another process during command handling")
    val user = userRepository.aggregateRootOf(AggregateId.of("some-aggregate-id"))
    eventStore.toggleOnOptimisticConcurrencyExceptionOnSave()

    When("sending a command")
    val result = await(user.handle(
      CommandEnvelope.of(
        CommandId.of("some_command_id"),
        RegisterUser.builder()
          .username("joebloggs")
          .password("password")
          .create())))

    Then("the result should report a concurrent modification")
    result.get() shouldBe an[ConcurrentModificationResult[_,_]]
  }

  it should "propagate error when event store fails to save generated events" in {
    Given("the event store can't load events")
    eventStore.toggleSaveErrorStateOn()

    And("an aggregate")
    val user = userRepository.aggregateRootOf(AggregateId.of("some-aggregate-id"))

    When("sending a command")
    val result = await(user.handle(
      CommandEnvelope.of(
        CommandId.of("some_command_id"),
        RegisterUser.builder()
          .username("joebloggs")
          .password("password")
          .create())))

    Then("the command should fail")
    result.isSuccess should be(false)

    And("the event store's error should be returned")
    result.getCause.get shouldBe an[IllegalStateException]
  }

  it should "process a duplicate command if the deduplication strategy says it's ok to process it" in {
    Given("the deduplication strategy is configured to not perform deduplication")
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
    await(user.handle(CommandEnvelope.of(CommandId.of("some_command_id"), ChangePassword.builder().password("newPassword").create())))
    val result = await(user.handle(
      CommandEnvelope.of(
        CommandId.of("command_id"),
        ChangePassword.builder()
          .password("anotherNewPassword")
          .create())))

    Then("the second command will not be deduplicated")
    val successResult = result.asInstanceOf[Future[SuccessResult[_,_]]]
    successResult.get().generatedEvents().size() should be(1)
    successResult.get().generatedEvents().get(0) should be(PasswordChanged.builder()
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

  class Reporter extends AggregateRepositoryReporter {
    override def createProbe[A <: Aggregate[C, E, State], C <: DomainCommand, E <: DomainEvent, State](aggregateType: AggregateType[A, C, E, State], aggregateId: AggregateId): CommandHandlingProbe[A, C, E, State] = {
      new CommandHandlingProbe[A,C,E,State] {

        override def startedHandling(command: CommandEnvelope[C]) = println(s"Started handling: $command")

        override def startedLoadingEvents() = println(s"Started loading previous events")

        override def finishedLoadingEvents(previousEvents: collection.List[PersistedEvent[A, E]]) = println(s"Loaded previous events $previousEvents")

        override def finishedLoadingEvents(unexpectedException: Throwable) = println(s"Failed to load previous events due to: ${unexpectedException.getClass.getName}")

        override def startedApplyingCommand() = println(s"Started applying command")

        override def commandApplicationAccepted(events: collection.List[_ >: E], deduplicated: Boolean) = println(s"Command accepted: $events")

        override def commandApplicationRejected(rejection: Throwable, deduplicated: Boolean) = println(s"Command rejected: ${rejection.getClass.getName}")

        override def commandApplicationFailed(unexpectedException: Throwable) = println(s"Command failed: ${unexpectedException.getClass.getName}")

        override def startedPersistingEvents(events: collection.List[_ >: E], expectedSequenceNumber: Long) = println(s"Started persisting events: $events")

        override def startedPersistingEvents(events: collection.List[_ >: E], state: State, expectedSequenceNumber: Long) = println(s"Started persisting events and state: $events, $state")

        override def finishedPersistingEvents(persistedEvents: collection.List[PersistedEvent[A, E]]) = println(s"Finished persisting events: $persistedEvents")

        override def finishedPersistingEvents(unexpectedException: Throwable) = println(s"Failed to persist events:  ${unexpectedException.getClass.getName}")

        override def finishedHandling(result: AggregateRepository.CommandHandlingResult[C, E]) = println(s"Finished command handling: ${result.getClass.getName}")

        override def finishedHandling(unexpectedException: Throwable) = println(s"Command handling failed: ${unexpectedException.getClass.getName}")
      }
    }
  }

}
