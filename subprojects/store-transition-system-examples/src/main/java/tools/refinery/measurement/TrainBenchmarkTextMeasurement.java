package tools.refinery.measurement;

import com.google.inject.Inject;
import tools.refinery.generator.ProblemLoader;
import tools.refinery.language.semantics.ModelInitializer;
import tools.refinery.store.dse.modification.ModificationAdapter;
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

public class TrainBenchmarkTextMeasurement extends MeasurementBase {
	@Inject
	private ProblemLoader loader;

	@Inject
	private ModelInitializer modelInitializer;

	public void init(int size, boolean visualize) throws IOException {
		this.visualize = visualize;
		if (visualize) {
			visualizationOutputPath = "output_visualization/trainbenchmark_" + size;
		}
		loader.extraPath("input/models/trainbenchmark/");
		var problem = loader.loadFile("input/models/trainbenchmark/size-" + size + "/railway-batch-" + size + ".problem");

		storeBuilder = storeBuilder
				.with(QueryInterpreterAdapter.builder())
				.with(PropagationAdapter.builder())
				.with(StateCoderAdapter.builder().
						stateCodeCalculatorFactory(PartialNeighborhoodCalculator.factory(Concreteness.PARTIAL, 7)))
				.with(ModificationAdapter.builder())
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
