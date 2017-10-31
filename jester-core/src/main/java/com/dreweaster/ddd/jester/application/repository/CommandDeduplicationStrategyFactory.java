package com.dreweaster.ddd.jester.application.repository;

public interface CommandDeduplicationStrategyFactory {

    /**
     * Creates a @see CommandDeduplicationStrategy. A strategy should not be cached and should be created
     * on demand as needed. Typically, this means creating a strategy for each command handling invocation.
     *
     * @return a @see CommandDeduplicationStrategy
     */
    CommandDeduplicationStrategyBuilder newBuilder();
}
