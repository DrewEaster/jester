package com.dreweaster.ddd.jester.application.repository.deduplicating;

import com.dreweaster.ddd.jester.application.eventstore.PersistedEvent;
import com.dreweaster.ddd.jester.domain.CausationId;
import com.dreweaster.ddd.jester.domain.CommandId;
import io.vavr.collection.HashSet;
import io.vavr.collection.Set;

import java.time.Instant;
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

        private Instant barrierDate;

        private Set<CausationId> causationIds = HashSet.empty();

        public Builder(Instant barrierDate) {
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
