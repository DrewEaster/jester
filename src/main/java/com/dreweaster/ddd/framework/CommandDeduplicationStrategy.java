package com.dreweaster.ddd.framework;

public interface CommandDeduplicationStrategy {

    boolean isDuplicate(CommandId commandId);
}
