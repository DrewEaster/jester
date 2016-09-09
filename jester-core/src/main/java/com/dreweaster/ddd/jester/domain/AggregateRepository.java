package com.dreweaster.ddd.jester.domain;

import javaslang.collection.List;
import javaslang.concurrent.Future;
import javaslang.control.Option;

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

    interface AggregateRoot<C extends DomainCommand, E extends DomainEvent> {

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

        Future<List<? super E>> handle(CommandEnvelope<C> commandEnvelope);
    }

    AggregateRoot<C, E> aggregateRootOf(AggregateId aggregateId);
}
