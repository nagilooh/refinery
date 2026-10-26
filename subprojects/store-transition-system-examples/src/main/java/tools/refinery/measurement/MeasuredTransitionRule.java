package tools.refinery.measurement;

import tools.refinery.store.transition.system.statespace.Transition;
import tools.refinery.store.tuple.Tuple;

import java.util.ArrayList;
import java.util.List;

public record MeasuredTransitionRule(Transition transition, TransitionType transitionType) {

	public boolean fire(Tuple tuple) {
		switch (transitionType) {
			case FIRE_ONE -> {
				return transition.fireAction(tuple);
			}
			case FIRE_ALL -> {
				var activations = getAllActivationsWithPartialMatch(tuple);
				return fireAll(activations);
			}
			default -> throw new IllegalArgumentException("Unknown transition type: " + transitionType);
		}
	}

	// TODO: Could be public and measured directly, excluding the collection of the activations.
	private boolean fireAll(List<Tuple> tuples) {
		if (transitionType != TransitionType.FIRE_ALL) {
			throw new IllegalArgumentException("fireAll is false, cannot call fireAll.");
		}
		for (var tuple : tuples) {
			if (!transition().fireAction(tuple)) {
				return false;
			}
		}
		return true;
	}

	public List<Tuple> getAllActivationsWithPartialMatch(Tuple tuple) {
		if (transitionType != TransitionType.FIRE_ALL) {
			return List.of(tuple);
		}
		var activations = new ArrayList<Tuple>();
		var fixedParametersSize = tuple.getSize();
		var resultSet = transition.getAllActivationsAsResultSet();
		var activationCursor = resultSet.getAll();
		while (activationCursor.move()) {
			var activation = activationCursor.getKey();
			if (activation.getSize() <= fixedParametersSize) {
				throw new IllegalArgumentException("Activation tuple size is less than or equal to the fixed parameters size.");
			}
			var matchingParameters = true;
			for (int i = 0; i < fixedParametersSize; i++) {
				if (activation.get(i) != tuple.get(i)) {
					matchingParameters = false;
					break;
				}
			}
			if (!matchingParameters) {
				continue;
			}
			activations.add(activation);
		}
		return activations;
	}
}
