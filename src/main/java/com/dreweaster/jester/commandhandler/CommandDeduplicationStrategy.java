package com.dreweaster.jester.commandhandler;

import com.dreweaster.jester.domain.CommandId;

public interface CommandDeduplicationStrategy {

    boolean isDuplicate(CommandId commandId);
}
