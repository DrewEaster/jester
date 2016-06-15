package com.dreweaster.ddd.framework;

import rx.Single;

import java.util.List;

public interface AggregateRootRef<E> {

    <C extends DomainCommand> Single<List<E>> handle(C command);
}
