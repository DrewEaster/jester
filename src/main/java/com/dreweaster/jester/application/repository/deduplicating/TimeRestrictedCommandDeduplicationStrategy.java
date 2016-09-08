package com.dreweaster.jester.application.repository.deduplicating;

import com.dreweaster.jester.application.eventstore.PersistedEvent;
import com.dreweaster.jester.domain.CausationId;
import com.dreweaster.jester.domain.CommandId;
import javaslang.collection.HashSet;
import javaslang.collection.Set;

import java.time.LocalDateTime;

public class TimeRestrictedCommandDeduplicationStrategy implements CommandDeduplicationStrategy {

    private Set<CausationId> causationIds = HashSet.empty();

    private TimeRestrictedCommandDeduplicationStrategy(Set<CausationId> causationIds) {
        this.causationIds = causationIds;
    }

    @Override
    public boolean isDuplicate(CommandId commandId) {
        return causationIds.contains(CausationId.of(commandId.get()));
    }

    public static class Builder implements CommandDeduplicationStrategyBuilder {

        private LocalDateTime barrierDate;

        private Set<CausationId> causationIds = HashSet.empty();

        public Builder(LocalDateTime barrierDate) {
            this.barrierDate = barrierDate;
        }

        @Override
        public CommandDeduplicationStrategyBuilder addEvent(PersistedEvent<?, ?> domainEvent) {
            if (domainEvent.timestamp().isAfter(barrierDate)) {
                causationIds = causationIds.add(domainEvent.causationId());
            }
            return this;
        }

        @Override
        public CommandDeduplicationStrategy build() {
            return new TimeRestrictedCommandDeduplicationStrategy(causationIds);
        }
    }
}
