package com.dreweaster.ddd.framework;

import java.util.List;
import java.util.concurrent.CompletionStage;

public interface AggregateRootRef<C, E> {

    CompletionStage<List<E>> handle(C command);
}
