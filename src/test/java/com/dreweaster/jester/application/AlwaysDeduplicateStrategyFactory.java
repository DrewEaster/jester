package com.dreweaster.jester.application;

import com.dreweaster.jester.application.eventstore.PersistedEvent;
import com.dreweaster.jester.application.repository.deduplicating.CommandDeduplicationStrategy;
import com.dreweaster.jester.application.repository.deduplicating.CommandDeduplicationStrategyBuilder;
import com.dreweaster.jester.application.repository.deduplicating.CommandDeduplicationStrategyFactory;
import com.dreweaster.jester.domain.CommandId;

import java.util.ArrayList;
import java.util.List;

public final class AlwaysDeduplicateStrategyFactory implements CommandDeduplicationStrategyFactory {

    @Override
    public CommandDeduplicationStrategyBuilder newBuilder() {
        return new CommandDeduplicationStrategyBuilder() {

            private List<CommandId> commandIdList = new ArrayList<>();

            @Override
            public CommandDeduplicationStrategyBuilder addEvent(PersistedEvent<?, ?> domainEvent) {
                commandIdList.add(domainEvent.commandId());
                return this;
            }

            @Override
            public CommandDeduplicationStrategy build() {
                return commandIdList::contains;
            }
        };
    }
}
