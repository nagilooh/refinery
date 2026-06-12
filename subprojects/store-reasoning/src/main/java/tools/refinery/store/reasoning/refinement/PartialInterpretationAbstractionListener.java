package tools.refinery.store.reasoning.refinement;

import tools.refinery.logic.AbstractValue;
import tools.refinery.store.tuple.Tuple;

@FunctionalInterface
public interface PartialInterpretationAbstractionListener<A extends AbstractValue<A, C>, C> {

	boolean join(Tuple key, A fromValue, A toValue);
}
