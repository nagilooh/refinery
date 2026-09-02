package tools.refinery.store.transition.system;

import tools.refinery.store.adapter.ModelAdapterBuilder;
import tools.refinery.store.dse.transition.objectives.Criterion;
import tools.refinery.store.dse.transition.statespace.StateSpaceStore;
import tools.refinery.store.transition.system.statespace.TransitionRule;

public interface TransitionSystemBuilder extends ModelAdapterBuilder {

	TransitionSystemBuilder transition(TransitionRule rule);

	TransitionSystemBuilder accept(Criterion criterion);

	TransitionSystemBuilder with(StateSpaceStore stateSpaceStore);
}
