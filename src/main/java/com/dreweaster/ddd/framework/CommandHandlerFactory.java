package com.dreweaster.ddd.framework;

/**
 */
public interface CommandHandlerFactory {

    <A extends Aggregate<E, ?>, E extends DomainEvent> CommandHandler<A, E> handlerFor(Class<A> aggregateType);

    /**
     * A read-only command handler allows read only commands to be sent to a specific aggregate instance. This allows
     * state to be fetched from a specific aggregate instance when there's a need for a consistent read by aggregate id
     * - i.e. a read only command is capable of returning a consistent view of the state of the aggregate instance
     * rather than an eventually consistent view that a typical read model provides. The obvious limitation here is that
     * the state returned is limited to what's internally available within the consistency boundary of the specific
     * aggregate instance. Read models are tuned for more powerful queries across the state of multiple aggregates,
     * but you are trading consistency for that power in those cases.
     */
    <A extends Aggregate<E, ?>, E extends DomainEvent> ReadOnlyCommandHandler<A, E> readOnlyHandlerFor(Class<A> aggregateType);
}
