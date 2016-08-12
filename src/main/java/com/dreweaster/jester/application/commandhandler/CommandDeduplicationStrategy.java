package com.dreweaster.jester.application.commandhandler;

import com.dreweaster.jester.domain.CommandId;

public interface CommandDeduplicationStrategy {

    boolean isDuplicate(CommandId commandId);
}
