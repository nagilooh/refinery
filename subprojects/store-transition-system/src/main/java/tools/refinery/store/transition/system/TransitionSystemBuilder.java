package tools.refinery.store.transition.system;

import tools.refinery.logic.term.truthvalue.TruthValue;
import tools.refinery.store.adapter.ModelAdapterBuilder;
import tools.refinery.store.dse.transition.DesignSpaceExplorationBuilder;
import tools.refinery.store.dse.transition.Rule;
import tools.refinery.store.dse.transition.objectives.Criterion;
import tools.refinery.store.dse.transition.objectives.Objective;
import tools.refinery.store.reasoning.representation.PartialSymbol;
import tools.refinery.store.representation.Symbol;

public interface TransitionSystemBuilder extends DesignSpaceExplorationBuilder {

	TransitionSystemBuilder transition(Rule rule);

//	TransitionSystemBuilder accept(Criterion criterion);

//	TransitionSystemBuilder exclude(Criterion criterion);

//	TransitionSystemBuilder objective(Objective objective);
}
