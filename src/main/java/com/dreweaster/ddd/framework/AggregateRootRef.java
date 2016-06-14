package com.dreweaster.ddd.framework;

import java.util.List;

public interface AggregateRootRef<E> {

    <C extends DomainCommand> List<E> handle(C command);

    <C extends ReadOnlyDomainCommand<O>, O> O handleReadOnly(C command);
}
