package com.dreweaster.ddd.framework;

import java.util.List;

public interface CommandHandler<A, C, E> {

    List<DomainEvent<A, E>> handle(Command<C> command);
}
