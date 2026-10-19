package tools.refinery.measurement;

import com.google.inject.Inject;
import tools.refinery.generator.ProblemLoader;
import tools.refinery.language.semantics.ModelInitializer;
import tools.refinery.store.dse.propagation.PropagationAdapter;
import tools.refinery.store.dse.transition.DesignSpaceExplorationAdapter;
import tools.refinery.store.query.interpreter.QueryInterpreterAdapter;
import tools.refinery.store.reasoning.ReasoningAdapter;
import tools.refinery.store.reasoning.interpretation.PartialNeighborhoodCalculator;
import tools.refinery.store.reasoning.literal.Concreteness;
import tools.refinery.store.statecoding.StateCoderAdapter;
import tools.refinery.store.transition.system.TransitionSystemAdapter;
import tools.refinery.store.transition.system.strategy.TransitionSystemStoreManager;

import java.io.IOException;

public class StatechartTextMeasurement extends MeasurementBase {
	@Inject
	private ProblemLoader loader;

	@Inject
	private ModelInitializer modelInitializer;

	public void init(int size, MeasurementScenario scenario, boolean visualize) throws IOException {
		this.visualize = visualize;
		if (visualize) {
			visualizationOutputPath = "output_visualization/statechart_" + size;
		}
		var problem = loader.loadString("""
				import builtin::strategy.

			   class State {
			   }

			   class InitialState extends State.

			   class Transition {
				   State[1] source
				   State[1] target
			   }

			   pred active(State s).

			   declare redState, redYellowState, greenState, yellowState, t1, t2, t3, t4.
			   !exists(State::new).
			   !exists(InitialState::new).
			   !exists(Transition::new).
			   InitialState(redState).
			   State(redYellowState).
			   !InitialState(redYellowState).
			   State(greenState).
			   !InitialState(greenState).
			   State(yellowState).
			   !InitialState(yellowState).
			   Transition(t1).
			   Transition(t2).
			   Transition(t3).
			   Transition(t4).
			   default !source(*, *).
			   source(t1, redState).
			   source(t2, redYellowState).
			   source(t3, greenState).
			   source(t4, yellowState).
			   default !target(*, *).
			   target(t1, redYellowState).
			   target(t2, greenState).
			   target(t3, yellowState).
			   target(t4, redState).
			   default active(*): false.
			   active(redState): true.

			   transition rule stateTransition(Transition t, State s1, State s2) <->
				   source(t, s1),
				   target(t, s2),
				   active(s1)
			   ==>
				   !active(s1),
				   active(s2).


			   @target
			   pred neverEnd() <->
				   false.
				""");



		storeBuilder = storeBuilder
				.with(QueryInterpreterAdapter.builder())
				.with(PropagationAdapter.builder())
				.with(StateCoderAdapter.builder().
						stateCodeCalculatorFactory(PartialNeighborhoodCalculator.factory(Concreteness.PARTIAL, 7)))
				.with(DesignSpaceExplorationAdapter.builder())
				.with(TransitionSystemAdapter.builder())
				.with(ReasoningAdapter.builder());

		modelSeed = modelInitializer.createModel(problem, storeBuilder);
		super.init();
	}

	@Override
	protected boolean renderTransitionsToAlreadyVisitedStates() {
		return true;
	}

	@Override
	public void runTransitionSystem() {
		var manager = new TransitionSystemStoreManager(store);
		manager.startExploration(this.currentVersion);
		var solution = manager.getSolution();
		System.out.println("Solution found: " + solution);
	}
}
