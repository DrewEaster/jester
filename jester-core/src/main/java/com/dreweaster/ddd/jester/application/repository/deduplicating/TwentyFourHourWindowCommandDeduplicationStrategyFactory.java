package com.dreweaster.ddd.jester.application.repository.deduplicating;

import java.time.LocalDateTime;

public class TwentyFourHourWindowCommandDeduplicationStrategyFactory implements CommandDeduplicationStrategyFactory {

    @Override
    public CommandDeduplicationStrategyBuilder newBuilder() {
        // TODO: Defaulting to 24 hours, but could be configurable.
        // TODO: Should be configured to match length of time events remain in streams (e.g. Kinesis default is 24 hours)
        return new TimeRestrictedCommandDeduplicationStrategy.Builder(LocalDateTime.now().minusDays(1));
    }
}
