package tools.refinery.measurement;

import com.google.inject.Inject;
import tools.refinery.generator.ProblemLoader;
import tools.refinery.language.semantics.ModelInitializer;
import tools.refinery.store.dse.propagation.PropagationAdapter;
import tools.refinery.store.dse.transition.DesignSpaceExplorationAdapter;
import tools.refinery.store.model.ModelStore;
import tools.refinery.store.query.ModelQueryAdapter;
import tools.refinery.store.query.interpreter.QueryInterpreterAdapter;
import tools.refinery.store.reasoning.ReasoningAdapter;
import tools.refinery.store.reasoning.ReasoningStoreAdapter;
import tools.refinery.store.statecoding.StateCoderAdapter;
import tools.refinery.store.transition.system.TransitionSystemAdapter;
import tools.refinery.store.transition.system.strategy.TransitionSystemStoreManager;
import tools.refinery.visualization.ModelVisualizerAdapter;
import tools.refinery.visualization.internal.FileFormat;

import java.io.IOException;

public class DiningPhilosophersMeasurement extends MeasurementBase {
	@Inject
	private ProblemLoader loader;

	@Inject
	private ModelInitializer modelInitializer;

	public void init(int size) throws IOException {
		this.visualize = true;
		var problem = loader.loadString("""
				import builtin::strategy.

				class Phil {
					Fork[1] left
					Fork[1] right
					Fork[0..2] hold
					Status[1] status
				}

				class Fork.

				enum Status {
					think,
					hungry,
					hasL,
					hasR,
					eat
				}

				transition rule getHungry(Phil p) <->
					status(p, think)
				==>
					!status(p, think),
					status(p, hungry).

				transition rule getLeftFork(Phil p1, Fork f) <->
					status(p1, hungry),
					left(p1, f),
					right(p2, f),
					!hold(p2, f)
				==>
					hold(p1, f),
					!status(p1, hungry),
					status(p1, hasL).

				transition rule getRightFork(Phil p1, Fork f) <->
					status(p1, hasL),
					right(p1, f),
					left(p2, f),
					!hold(p2, f)
				==>
					hold(p1, f),
					!status(p1, hasL),
					status(p1, eat).

				transition rule finishEating(Phil p, Fork f) <->
					status(p, eat),
					left(p, f),
					hold(p, f)
				==>
					!hold(p, f),
					!status(p, eat),
					status(p, hasR).

				transition rule releaseRightFork(Phil p, Fork f) <->
					status(p, hasR),
					right(p, f),
					hold(p, f)
				==>
					!hold(p, f),
					!status(p, hasR),
					status(p, think).

				@target
				pred heldByTwo(Fork f) <->
					hold(p1, f),
					hold(p2, f),
					p1 != p2.

				Phil(ph1).
				Phil(ph2).
				Phil(ph3).
				Fork(f1).
				Fork(f2).
				Fork(f3).

				left(ph1, f1).
				left(ph2, f2).
				left(ph3, f3).
				right(ph1, f3).
				right(ph2, f1).
				right(ph3, f2).

				status(ph1, think).
				status(ph2, think).
				status(ph3, think).
				default !hold(*, *).

				!exists(Phil::new).
				!exists(Fork::new).
				""");



		var storeBuilder = ModelStore.builder()
				.with(QueryInterpreterAdapter.builder())
				.with(PropagationAdapter.builder())
				.with(StateCoderAdapter.builder())
				.with(DesignSpaceExplorationAdapter.builder())
				.with(TransitionSystemAdapter.builder())
				.with(ReasoningAdapter.builder());

		if (visualize) {
			storeBuilder = storeBuilder.with(ModelVisualizerAdapter.builder()
					.withOutputPath("test_output_ph")
					.withFormat(FileFormat.SVG)
					.saveStates()
					.saveDesignSpace()
			);
		}
		var seed = modelInitializer.createModel(problem, storeBuilder);

		store = storeBuilder.build();

//        System.out.println("model seed built, creating model");
        this.model = store.getAdapter(ReasoningStoreAdapter.class).tryCreateInitialModel(seed).model();
//        System.out.println("model created, committing initial state");
        this.queryEngine = model.getAdapter(ModelQueryAdapter.class);
        this.currentVersion = model.commit();
//        System.out.println("initial state committed, flushing changes");
        this.queryEngine.flushChanges();
//        System.out.println("changes flushed, getting adapters");
        this.propagationAdapter = model.getAdapter(PropagationAdapter.class);
        this.dseAdapter = model.getAdapter(DesignSpaceExplorationAdapter.class);
        this.tsAdapter = model.getAdapter(TransitionSystemAdapter.class);


		if (visualize) {
			this.modelVisualizerAdapter = model.getAdapter(ModelVisualizerAdapter.class);
			this.stateSpaceStore = model.getAdapter(DesignSpaceExplorationAdapter.class).getStateSpaceStore();
			this.stateCoderAdapter = model.getAdapter(StateCoderAdapter.class);

//			stateSpaceStore.addState(currentVersion, stateCoderAdapter.calculateModelCode(), false, false);
		}
    }

	@Override
	protected boolean visualizeParameterForFinish() {
		return true;
	}

	@Override
	public void runTransitionSystem() {
//		super.runTransitionSystem();
		var manager = new TransitionSystemStoreManager(store);
		// startExploration already invoked by super; retrieve solution and print (original behavior)
		manager.startExploration(this.currentVersion);
		var solution = manager.getSolution();
		System.out.println("Solution found: " + solution);

        modelVisualizerAdapter.visualize(stateSpaceStore, true);
	}
}
