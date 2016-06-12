package com.dreweaster.ddd.framework;

import java.util.List;

public interface AggregateRootRef<C, E> {

    List<E> handle(C command);
}
