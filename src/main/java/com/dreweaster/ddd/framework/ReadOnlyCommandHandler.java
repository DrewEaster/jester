package com.dreweaster.ddd.framework;

public interface ReadOnlyCommandHandler<C, R> {

    R handle(Command<C> command);
}
