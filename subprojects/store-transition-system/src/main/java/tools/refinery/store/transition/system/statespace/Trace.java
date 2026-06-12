package tools.refinery.store.transition.system.statespace;

import java.util.ArrayList;
import java.util.List;

public record Trace(List<State> states, List<FiredTransition> transitions) {

	public static Trace of(State target) {
		List<State> states = new ArrayList<>();
		List<FiredTransition> transitions = new ArrayList<>();
		states.add(target);
		while (target.incomingTransition() != null) {
			var transition = target.incomingTransition();
			var parent = target.parent();
			transitions.addFirst(transition);
			states.addFirst(parent);
			target = parent;
		}
		return new Trace(states, transitions);
	}

	public static Trace of(List<State> states, List<FiredTransition> transitions) {
		if (states.size() != transitions.size() + 1) {
			throw new IllegalArgumentException("The number of states should be one more than the number of transitions!");
		}
		return new Trace(states, transitions);
	}
}
