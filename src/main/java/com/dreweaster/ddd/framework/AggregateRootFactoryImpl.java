package com.dreweaster.ddd.framework;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

public class AggregateRootFactoryImpl implements AggregateRootFactory {

    @Override
    public <A extends Aggregate<C, E, State>, C extends DomainCommand, E extends DomainEvent, State> AggregateRootRef<C, E> aggregateOf(
            Class<A> aggregateType,
            AggregateId aggregateId,
            List<E> previousEvents) {

        try {
            A aggregateInstance = aggregateType.newInstance();

            // TODO: Pass snapshot once implemented
            Behaviour<C, E, State> behaviour = aggregateInstance.initialBehaviour(Optional.empty());

            for (E event : previousEvents) {
                behaviour = behaviour.handleEvent(event);
            }

            return new AggregateRootRefImpl<>(aggregateId, behaviour);
        } catch (Exception ex) {
            // TODO: Need to handle this properly - happens if can't instantiate aggregate instance from class
            throw new RuntimeException(ex);
        }
    }

    private class AggregateRootRefImpl<A extends Aggregate<C, E, State>, C extends DomainCommand, E extends DomainEvent, State> implements AggregateRootRef<C, E> {

        private AggregateId aggregateId;

        private Behaviour<C, E, State> behaviour;

        private boolean executed = false;

        public AggregateRootRefImpl(AggregateId aggregateId, Behaviour<C, E, State> behaviour) {
            this.aggregateId = aggregateId;
            this.behaviour = behaviour;
        }

        @Override
        public CompletionStage<List<E>> handle(C command) {

            // AggregateRootRefs are one use only
            if (executed) {
                CompletableFuture<List<E>> completableFuture = new CompletableFuture<>();
                completableFuture.completeExceptionally(new IllegalStateException("This reference has already been used. Create another!"));
                return completableFuture;
            }

            CompletableFuture<List<E>> completableFuture = new CompletableFuture<>();

            behaviour.handleCommand(command, new CommandContext<E>() {

                @Override
                public AggregateId aggregateId() {
                    return aggregateId;
                }

                @Override
                public void success(List<E> events) {
                    completableFuture.complete(events);
                }

                @Override
                public void success(E event) {
                    completableFuture.complete(Collections.singletonList(event));
                }

                @Override
                public void error(Throwable error) {
                    completableFuture.completeExceptionally(error);
                }
            });

            executed = true;
            return completableFuture;
        }
    }
}
