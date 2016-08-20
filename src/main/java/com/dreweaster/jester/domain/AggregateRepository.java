package com.dreweaster.jester.domain;

import javaslang.collection.List;
import javaslang.concurrent.Future;

public interface AggregateRepository<A extends Aggregate<C, E, State>, C extends DomainCommand, E extends DomainEvent, State> {

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

        Future<List<? super E>> handle(CommandId commandId, C command);
    }

    AggregateRoot<C, E> aggregateRootOf(AggregateId aggregateId);
}
