package com.dreweaster.ddd.jester.domain;

import javaslang.collection.HashMap;
import javaslang.collection.List;
import javaslang.collection.Map;

public interface DomainModel {

    static <A extends Aggregate<C, E, State>, C extends DomainCommand, E extends DomainEvent, State> DomainModel of(
            AggregateType<A,C,E,State>... types) {
        SimpleDomainModel domainModel = new SimpleDomainModel();
        List.of(types).forEach(domainModel::registerAggregateType);
        return domainModel;
    }

    class SimpleDomainModel implements DomainModel {

        private Map<String, AggregateType> types = HashMap.empty();

        public <A extends Aggregate<C, E, State>, C extends DomainCommand, E extends DomainEvent, State> void registerAggregateType(
                AggregateType<A,C,E,State> type) {
            types = types.put(type.name(), type);
        }

        @SuppressWarnings("unchecked")
        @Override
        public <A extends Aggregate<C, E, State>, C extends DomainCommand, E extends DomainEvent, State> AggregateType<A, C, E, State> aggregateTypeFor(String name) {
            return (AggregateType<A, C, E, State>)types.apply(name);
        }
    }

    <A extends Aggregate<C, E, State>, C extends DomainCommand, E extends DomainEvent, State> AggregateType<A,C,E,State> aggregateTypeFor(String name);
}
