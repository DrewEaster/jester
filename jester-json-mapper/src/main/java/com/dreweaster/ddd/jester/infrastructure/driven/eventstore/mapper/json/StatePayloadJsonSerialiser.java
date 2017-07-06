package com.dreweaster.ddd.jester.infrastructure.driven.eventstore.mapper.json;

import com.dreweaster.ddd.jester.application.eventstore.PayloadMapper.PayloadSerialisationResult;
import com.dreweaster.ddd.jester.application.eventstore.SerialisationContentType;
import com.dreweaster.ddd.jester.domain.Aggregate;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 */
public abstract class StatePayloadJsonSerialiser<A extends Aggregate<?, ?, State>, State> {

     protected abstract Class<State> stateClass();

     protected abstract void doSerialise(State state, ObjectNode rootNode);

     public final PayloadSerialisationResult serialise(State state, ObjectNode rootNode) {
          doSerialise(state, rootNode);
          return PayloadSerialisationResult.of(rootNode.toString(), SerialisationContentType.JSON);
     }
}
