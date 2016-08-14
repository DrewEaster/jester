package com.dreweaster.jester.application.repository.deduplicating;

import com.dreweaster.jester.domain.CommandId;

public interface CommandDeduplicationStrategy {

    boolean isDuplicate(CommandId commandId);
}
