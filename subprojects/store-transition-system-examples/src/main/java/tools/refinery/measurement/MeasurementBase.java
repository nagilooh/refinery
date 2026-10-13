package tools.refinery.measurement;

import tools.refinery.store.dse.propagation.PropagationAdapter;
import tools.refinery.store.dse.transition.DesignSpaceExplorationAdapter;
import tools.refinery.store.dse.transition.statespace.ActivationStore;
import tools.refinery.store.map.Version;
import tools.refinery.store.model.Model;
import tools.refinery.store.model.ModelStore;
import tools.refinery.store.model.ModelStoreBuilder;
import tools.refinery.store.query.ModelQueryAdapter;
import tools.refinery.store.reasoning.ReasoningStoreAdapter;
import tools.refinery.store.reasoning.seed.ModelSeed;
import tools.refinery.store.statecoding.StateCoderAdapter;
import tools.refinery.store.transition.system.TransitionSystemAdapter;
import tools.refinery.store.transition.system.strategy.TransitionSystemStoreManager;
import tools.refinery.store.dse.transition.statespace.StateSpaceStore;
import tools.refinery.visualization.ModelVisualizerAdapter;
import tools.refinery.visualization.internal.FileFormat;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Common base class for measurement classes. Extracts shared fields and behavior used by
 * the various measurement implementations (e.g. step, run, finish, transition system start,
 * activation collection).
 */
public abstract class MeasurementBase implements MeasurementCase {
    protected boolean visualize = true;

    protected ModelStore store;
    protected ModelQueryAdapter queryEngine;
    protected PropagationAdapter propagationAdapter;
    protected DesignSpaceExplorationAdapter dseAdapter;
    protected TransitionSystemAdapter tsAdapter;
	protected ModelSeed modelSeed;
    protected Model model;
    protected final Random random = new Random(1);
    protected Version currentVersion;
    protected ModelVisualizerAdapter modelVisualizerAdapter;
    protected StateSpaceStore stateSpaceStore;
    protected StateCoderAdapter stateCoderAdapter;
	protected ModelStoreBuilder storeBuilder = ModelStore.builder();
	protected String visualizationOutputPath = "output";

	protected void init() {
		if (visualize) {
			storeBuilder = storeBuilder.with(ModelVisualizerAdapter.builder()
					.withOutputPath(visualizationOutputPath)
					.withFormat(FileFormat.DOT)
					.withFormat(FileFormat.SVG)
					.saveStates()
					.saveDesignSpace()
			);
		}
		this.store = storeBuilder.build();
		this.model = store.getAdapter(ReasoningStoreAdapter.class).tryCreateInitialModel(modelSeed).model();
		this.queryEngine = model.getAdapter(ModelQueryAdapter.class);
		this.currentVersion = model.commit();
		this.queryEngine.flushChanges();
		this.propagationAdapter = model.getAdapter(PropagationAdapter.class);
		this.dseAdapter = model.getAdapter(DesignSpaceExplorationAdapter.class);
		this.tsAdapter = model.getAdapter(TransitionSystemAdapter.class);

		if (visualize) {
			this.modelVisualizerAdapter = model.getAdapter(ModelVisualizerAdapter.class);
			this.stateSpaceStore = model.getAdapter(DesignSpaceExplorationAdapter.class).getStateSpaceStore();
			this.stateCoderAdapter = model.getAdapter(StateCoderAdapter.class);
		}
	}

    /**
     * Run a number of random exploration steps using the transition system activations.
     */
    public MeasurementResult run(int steps) {
		if (visualize) {
			stateSpaceStore.addState(currentVersion, stateCoderAdapter.calculateModelCode(), false, false);
		}
        List<Activation> activations = getActivations();
        for (int i = 0; i < steps; i++) {
			if (activations.isEmpty()) {
				System.out.println("No more activations available, stopping exploration.");
				return MeasurementResult.NO_MORE_TRANSITIONS;
			}
			var repairActivations =
					activations.stream().filter(a -> a.transformation().toString().contains("repair")).toList();
			if (!repairActivations.isEmpty()) {
				System.out.println("Repair activations available, prioritizing repair activations.");
				activations = repairActivations;
			}
            var randomActivation = activations.get((random.nextInt(activations.size())));
            step(randomActivation);
            activations = getActivations();
        }
		return MeasurementResult.MAX_STEPS_REACHED;
    }

    /**
     * Finish the measurement
     */
    public void finish() {
        if (visualize) {
            modelVisualizerAdapter.visualize(stateSpaceStore, renderTransitionsToAlreadyVisitedStates());
        }
        model.close();
    }

    /**
     * Start transition system exploration
     */
    public void runTransitionSystem() {
        var manager = new TransitionSystemStoreManager(store);
        manager.startExploration(this.currentVersion);
    }

    /**
     * Fire a single activation
     */
    protected void step(Activation activation) {
        Version lastVersion = null;
        if (visualize) {
            lastVersion = this.currentVersion;
        }
        var result = activation.fire();
		System.out.println("Fired activation: " + activation + ", result: " + result);
        propagationAdapter.propagate();
        dseAdapter.checkAccept();
        this.currentVersion = model.commit();
        if (visualize) {
            stateSpaceStore.addState(currentVersion, stateCoderAdapter.calculateModelCode(), false, false);
            stateSpaceStore.addTransition(lastVersion, this.currentVersion, new ActivationStore.VisitResult(
                    true, true, activation.transformationIndex(), activation.tupleIndex()));
        }
        queryEngine.flushChanges();
    }

    /**
     * Collect all activations from the transition system adapter.
     */
    protected List<Activation> getActivations() {
        List<Activation> activationList = new ArrayList<>();
        var transformationIndex = 0;
        for (var transformation : tsAdapter.getTransitions()) {
            var activationCursor = transformation.getAllActivationsAsResultSet().getAll();
            var tupleIndex = 0;
            while (activationCursor.move()) {
				var activation = new Activation(transformation, activationCursor.getKey(), transformationIndex, tupleIndex);
				System.out.println("Activation found: " + activation);
                activationList.add(activation);
                tupleIndex++;
            }
            transformationIndex++;
        }
        return activationList;
    }

    /**
     * Subclasses control the boolean parameter passed to the model visualizer when finishing.
     */
    protected abstract boolean renderTransitionsToAlreadyVisitedStates();
}

