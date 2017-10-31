package com.dreweaster.ddd.jester.domain;

import io.vavr.collection.List;
import io.vavr.concurrent.Future;
import io.vavr.control.Option;

import java.util.Optional;

public interface AggregateRepository<A extends Aggregate<C, E, State>, C extends DomainCommand, E extends DomainEvent, State> {

    class CommandEnvelope<C> {

        public static <C extends DomainCommand> CommandEnvelope<C> of(CommandId id, C command) {
            return new CommandEnvelope<>(id, command, Option.none(), Option.none());
        }

        public static <C extends DomainCommand> CommandEnvelope<C> of(CommandId id, C command, CorrelationId correlationId) {
            return new CommandEnvelope<>(id, command, Option.none(), Option.of(correlationId));
        }

        public static <C extends DomainCommand> CommandEnvelope<C> of(CommandId id, C command, CausationId causationId, CorrelationId correlationId) {
            return new CommandEnvelope<>(id, command, Option.of(causationId), Option.of(correlationId));
        }

        private CommandId commandId;

        private C command;

        private Option<CausationId> causationId = Option.none();

        private Option<CorrelationId> correlationId = Option.none();

        private CommandEnvelope(
                CommandId commandId,
                C command,
                Option<CausationId> causationId,
                Option<CorrelationId> correlationId) {

            this.commandId = commandId;
            this.command = command;
            this.causationId = causationId;
            this.correlationId = correlationId;
        }

        public CommandId commandId() {
            return commandId;
        }

        public C command() {
            return command;
        }

        public Option<CausationId> causationId() {
            return causationId;
        }

        public Option<CorrelationId> correlationId() {
            return correlationId;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;

            CommandEnvelope<?> that = (CommandEnvelope<?>) o;

            if (!commandId.equals(that.commandId)) return false;
            if (!command.equals(that.command)) return false;
            if (!causationId.equals(that.causationId)) return false;
            return correlationId.equals(that.correlationId);

        }

        @Override
        public int hashCode() {
            int result = commandId.hashCode();
            result = 31 * result + command.hashCode();
            result = 31 * result + causationId.hashCode();
            result = 31 * result + correlationId.hashCode();
            return result;
        }

        @Override
        public String toString() {
            return "CommandEnvelope{" +
                    "commandId=" + commandId +
                    ", command=" + command +
                    ", causationId=" + causationId +
                    ", correlationId=" + correlationId +
                    '}';
        }
    }

    interface CommandHandlingResult<C extends DomainCommand, E extends DomainEvent> {

        CommandEnvelope<C> command();
    }

    class SuccessResult<C extends DomainCommand, E extends DomainEvent> implements CommandHandlingResult<C,E> {

        public static <C extends DomainCommand, E extends DomainEvent> SuccessResult<C,E> of(CommandEnvelope<C> commandEnvelope, List<E> generatedEvents) {
            return new SuccessResult<>(commandEnvelope, generatedEvents, false);
        }

        public static <C extends DomainCommand, E extends DomainEvent> SuccessResult<C,E> of(CommandEnvelope<C> commandEnvelope, List<E> generatedEvents, boolean deduplicated) {
            return new SuccessResult<>(commandEnvelope, generatedEvents, deduplicated);
        }

        private CommandEnvelope<C> commandEnvelope;

        private List<E> generatedEvents;

        private boolean deduplicated;

        private SuccessResult(CommandEnvelope<C> commandEnvelope, List<E> generatedEvents, boolean deduplicated) {
            this.commandEnvelope = commandEnvelope;
            this.generatedEvents = generatedEvents;
            this.deduplicated = deduplicated;
        }

        @Override
        public CommandEnvelope<C> command() {
            return commandEnvelope;
        }

        public List<E> generatedEvents() {
            return generatedEvents;
        }

        public boolean wasDeduplicated() {
            return deduplicated;
        }
    }

    class RejectionResult<C extends DomainCommand, E extends DomainEvent> implements CommandHandlingResult<C,E> {

        public static <C extends DomainCommand, E extends DomainEvent> RejectionResult<C,E> of(CommandEnvelope<C> commandEnvelope, Throwable error) {
            return new RejectionResult<>(commandEnvelope, error, false);
        }

        public static <C extends DomainCommand, E extends DomainEvent> RejectionResult<C,E> of(CommandEnvelope<C> commandEnvelope, Throwable error, boolean deduplicated) {
            return new RejectionResult<>(commandEnvelope, error, deduplicated);
        }

        private CommandEnvelope<C> commandEnvelope;

        private Throwable error;

        private boolean deduplicated;

        private RejectionResult(CommandEnvelope<C> commandEnvelope, Throwable error, boolean deduplicated) {
            this.commandEnvelope = commandEnvelope;
            this.error = error;
            this.deduplicated = deduplicated;
        }

        @Override
        public CommandEnvelope<C> command() {
            return commandEnvelope;
        }

        public Throwable error() {
            return error;
        }

        public boolean wasDeduplicated() {
            return deduplicated;
        }
    }

    class ConcurrentModificationResult<C extends DomainCommand, E extends DomainEvent> implements CommandHandlingResult<C,E> {

        public static <C extends DomainCommand, E extends DomainEvent> ConcurrentModificationResult<C,E> of(CommandEnvelope<C> commandEnvelope) {
            return new ConcurrentModificationResult<>(commandEnvelope);
        }

        private CommandEnvelope<C> commandEnvelope;

        private ConcurrentModificationResult(CommandEnvelope<C> commandEnvelope) {
            this.commandEnvelope = commandEnvelope;
        }

        @Override
        public CommandEnvelope<C> command() {
            return commandEnvelope;
        }
    }

    interface AggregateRoot<C extends DomainCommand, E extends DomainEvent, State> {

        class NoHandlerForCommand extends RuntimeException {

            public <C extends DomainCommand> NoHandlerForCommand(C command) {
                super("The current behaviour does not explicitly handle the command: " + command);
            }
        }

        class NoHandlerForEvent extends RuntimeException {

            public <E extends DomainEvent> NoHandlerForEvent(E event) {
                super("The current behaviour does not explicitly handle the event: " + event);
            }
        }

        Future<CommandHandlingResult<C, E>> handle(CommandEnvelope<C> commandEnvelope);

        Future<Option<State>> state();
    }

    AggregateRoot<C, E, State> aggregateRootOf(AggregateId aggregateId);
}
