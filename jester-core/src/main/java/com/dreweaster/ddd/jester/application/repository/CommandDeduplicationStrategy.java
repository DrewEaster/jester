package com.dreweaster.ddd.jester.application.repository;

import com.dreweaster.ddd.jester.domain.CommandId;

public interface CommandDeduplicationStrategy {

    boolean isDuplicate(CommandId commandId);
}
