package tools.refinery.measurement;

import tools.refinery.store.dse.propagation.PropagationAdapter;
import tools.refinery.store.dse.transition.DesignSpaceExplorationAdapter;
import tools.refinery.store.dse.transition.statespace.ActivationStore;
import tools.refinery.store.map.Version;
import tools.refinery.store.model.Model;
import tools.refinery.store.model.ModelStore;
import tools.refinery.store.query.ModelQueryAdapter;
import tools.refinery.store.statecoding.StateCoderAdapter;
import tools.refinery.store.transition.system.TransitionSystemAdapter;
import tools.refinery.store.transition.system.strategy.TransitionSystemStoreManager;
import tools.refinery.store.dse.transition.statespace.StateSpaceStore;
import tools.refinery.visualization.ModelVisualizerAdapter;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Common base class for measurement classes. Extracts shared fields and behavior used by
 * the various measurement implementations (e.g. step, run, finish, transition system start,
 * activation collection).
 */
public abstract class MeasurementBase {
    protected boolean visualize = false;

    protected ModelStore store;
    protected ModelQueryAdapter queryEngine;
    protected PropagationAdapter propagationAdapter;
    protected DesignSpaceExplorationAdapter dseAdapter;
    protected TransitionSystemAdapter tsAdapter;
    protected Model model;
    protected final Random random = new Random();
    protected Version currentVersion;
    protected ModelVisualizerAdapter modelVisualizerAdapter;
    protected StateSpaceStore stateSpaceStore;
    protected StateCoderAdapter stateCoderAdapter;

    /**
     * Run a number of random exploration steps using the transition system activations.
     * Subclasses can keep or override this behavior if they need different logging.
     */
    public void run(int steps) {
        List<Activation> activations = getActivations();
        for (int i = 0; i < steps && !activations.isEmpty(); i++) {
            var randomActivation = activations.get((random.nextInt(activations.size())));
            step(randomActivation);
            activations = getActivations();
        }
    }

    /**
     * Finish the measurement: optionally visualize and close the model.
     */
    public void finish() {
        if (visualize) {
            modelVisualizerAdapter.visualize(stateSpaceStore, visualizeParameterForFinish());
        }
        model.close();
    }

    /**
     * Start transition system exploration. Subclasses may override to add additional actions.
     */
    public void runTransitionSystem() {
        var manager = new TransitionSystemStoreManager(store);
        manager.startExploration(this.currentVersion);
    }

    /**
     * Fire a single activation, propagate, accept DSE, commit and optionally record visualization metadata.
     */
    protected void step(Activation activation) {
        Version lastVersion = null;
        if (visualize) {
            lastVersion = this.currentVersion;
        }
        activation.fire();
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
                activationList.add(new Activation(transformation, activationCursor.getKey(), transformationIndex, tupleIndex));
                tupleIndex++;
            }
            transformationIndex++;
        }
        return activationList;
    }

    /**
     * Subclasses control the boolean parameter passed to the model visualizer when finishing.
     */
    protected abstract boolean visualizeParameterForFinish();
}

