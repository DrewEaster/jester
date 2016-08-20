package com.dreweaster.jester.application.repository.deduplicating;

import com.dreweaster.jester.application.eventstore.PersistedEvent;
import com.dreweaster.jester.domain.CommandId;
import javaslang.collection.HashSet;
import javaslang.collection.Set;

import java.time.LocalDateTime;

public class TimeRestrictedCommandDeduplicationStrategy implements CommandDeduplicationStrategy {

    private Set<CommandId> commandIds = HashSet.empty();

    private TimeRestrictedCommandDeduplicationStrategy(Set<CommandId> commandIds) {
        this.commandIds = commandIds;
    }

    @Override
    public boolean isDuplicate(CommandId commandId) {
        return commandIds.contains(commandId);
    }

    public static class Builder implements CommandDeduplicationStrategyBuilder {

        private LocalDateTime barrierDate;

        private Set<CommandId> commandIds = HashSet.empty();

        public Builder(LocalDateTime barrierDate) {
            this.barrierDate = barrierDate;
        }

        @Override
        public CommandDeduplicationStrategyBuilder addEvent(PersistedEvent<?, ?> domainEvent) {
            if (domainEvent.timestamp().isAfter(barrierDate)) {
                commandIds = commandIds.add(domainEvent.commandId());
            }
            return this;
        }

        @Override
        public CommandDeduplicationStrategy build() {
            return new TimeRestrictedCommandDeduplicationStrategy(commandIds);
        }
    }
}
