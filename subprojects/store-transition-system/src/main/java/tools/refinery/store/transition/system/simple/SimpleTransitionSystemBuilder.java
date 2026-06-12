package tools.refinery.store.transition.system.simple;

import tools.refinery.store.dse.transition.DesignSpaceExplorationBuilder;
import tools.refinery.store.dse.transition.Rule;

public interface SimpleTransitionSystemBuilder extends DesignSpaceExplorationBuilder {

	SimpleTransitionSystemBuilder transition(Rule rule);

//	TransitionSystemBuilder accept(Criterion criterion);

//	TransitionSystemBuilder exclude(Criterion criterion);

//	TransitionSystemBuilder objective(Objective objective);
}
