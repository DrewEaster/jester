package com.dreweaster.jester.application;

import com.dreweaster.jester.application.eventstore.PersistedEvent;
import com.dreweaster.jester.application.repository.deduplicating.CommandDeduplicationStrategy;
import com.dreweaster.jester.application.repository.deduplicating.CommandDeduplicationStrategyBuilder;
import com.dreweaster.jester.application.repository.deduplicating.CommandDeduplicationStrategyFactory;

public final class NeverDeduplicateStrategyFactory implements CommandDeduplicationStrategyFactory {

    @Override
    public CommandDeduplicationStrategyBuilder newBuilder() {
        return new CommandDeduplicationStrategyBuilder() {
            @Override
            public CommandDeduplicationStrategyBuilder addEvent(PersistedEvent<?, ?> domainEvent) {
                return this;
            }

            @Override
            public CommandDeduplicationStrategy build() {
                return commandId -> false;
            }
        };
    }
}
