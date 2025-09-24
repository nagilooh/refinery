package tools.refinery.store.dse.transition.statespace.internal;

import tools.refinery.store.dse.transition.DecisionRule;
import tools.refinery.store.dse.transition.VersionWithObjectiveValue;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class CompleteActivationStoreImpl extends ActivationStoreImpl {

	private final List<Transition> transitions;

	public CompleteActivationStoreImpl(List<DecisionRule> transformations, Consumer<VersionWithObjectiveValue> actionWhenAllActivationVisited) {
		super(transformations, actionWhenAllActivationVisited);
		transitions = new ArrayList<>();
	}

	public List<Transition> getTransitions() {
		return transitions;
	}

}
