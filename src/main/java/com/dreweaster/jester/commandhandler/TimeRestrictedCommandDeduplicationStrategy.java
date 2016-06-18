package com.dreweaster.jester.commandhandler;

import com.dreweaster.jester.domain.CommandId;
import com.dreweaster.jester.eventstore.PersistedEvent;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

public class TimeRestrictedCommandDeduplicationStrategy implements CommandDeduplicationStrategy {

    private Set<CommandId> commandIds = new HashSet<CommandId>();

    private TimeRestrictedCommandDeduplicationStrategy(Set<CommandId> commandIds) {
        this.commandIds = commandIds;
    }

    @Override
    public boolean isDuplicate(CommandId commandId) {
        return commandIds.contains(commandId);
    }

    public static class Builder implements CommandDeduplicationStrategyBuilder {

        private LocalDate barrierDate;

        private Set<CommandId> commandIds = new HashSet<CommandId>();

        public Builder(LocalDate barrierDate) {
            this.barrierDate = barrierDate;
        }

        @Override
        public CommandDeduplicationStrategyBuilder addEvent(PersistedEvent<?, ?> domainEvent) {
            if (domainEvent.timestamp().isAfter(barrierDate)) {
                commandIds.add(domainEvent.commandId());
            }
            return this;
        }

        @Override
        public CommandDeduplicationStrategy build() {
            return new TimeRestrictedCommandDeduplicationStrategy(commandIds);
        }
    }
}
