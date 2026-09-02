package tools.refinery.store.transition.system.strategy;

import tools.refinery.store.dse.transition.VersionWithObjectiveValue;
import tools.refinery.store.dse.transition.statespace.ActivationStore;
import tools.refinery.store.dse.transition.statespace.EquivalenceClassStore;
import tools.refinery.store.dse.transition.statespace.ObjectivePriorityQueue;
import tools.refinery.store.dse.transition.statespace.internal.ActivationStoreImpl;
import tools.refinery.store.dse.transition.statespace.internal.FastEquivalenceClassStore;
import tools.refinery.store.dse.transition.statespace.internal.ObjectivePriorityQueueImpl;
import tools.refinery.store.map.Version;
import tools.refinery.store.model.ModelStore;
import tools.refinery.store.statecoding.StateCoderStoreAdapter;
import tools.refinery.store.transition.system.TransitionSystemStoreAdapter;
import tools.refinery.store.transition.system.statespace.State;
import tools.refinery.store.transition.system.statespace.Trace;
import tools.refinery.store.transition.system.statespace.Transition;

import java.util.Comparator;
import java.util.function.BiFunction;
import java.util.function.Consumer;

public class TransitionSystemStoreManager {

	private final ModelStore modelStore;
	private final ActivationStore<Version> activationStore;
	private final EquivalenceClassStore equivalenceClassStore;
	private final ObjectivePriorityQueue<State> objectiveStore;
	Trace solution;

	public TransitionSystemStoreManager(ModelStore modelStore) {
		this.modelStore = modelStore;

		var storeAdapter = modelStore.getAdapter(TransitionSystemStoreAdapter.class);
		this.objectiveStore = new ObjectivePriorityQueueImpl<>(Comparator.comparingInt(State::depth).reversed());
		BiFunction<Transition.Builder, Integer, Double> weightProvider = (_, unvisited) -> unvisited == 0 ? 0.0 : 1.0;
		Consumer<Version> whenAllActivationsVisited = x -> objectiveStore.removeIf(s -> x.equals(s.version()));
		this.activationStore = new ActivationStoreImpl<>(storeAdapter.getTransitions(), weightProvider,
				whenAllActivationsVisited);
		this.equivalenceClassStore = new FastEquivalenceClassStore(modelStore.getAdapter(StateCoderStoreAdapter.class)) {
			@Override
			protected void delegate(VersionWithObjectiveValue version, int[] emptyActivations, boolean accept) {
				throw new UnsupportedOperationException("This equivalence storage is not prepared to resolve " +
						"symmetries!");
			}
		};
	}

	public ActivationStore<Version> getActivationStore() {
		return activationStore;
	}

	public EquivalenceClassStore getEquivalenceClassStore() {
		return equivalenceClassStore;
	}

	public ObjectivePriorityQueue<State> getObjectiveStore() {
		return objectiveStore;
	}

	public void setSolution(Trace solution) {
		this.solution = solution;
	}

	public Trace getSolution() {
		return solution;
	}

	public void startExploration(Version initial) {
		startExploration(initial, 1);
	}

	public void startExploration(Version initial, long randomSeed) {
		try (var model = modelStore.createModelForState(initial)) {
			TransitionSystemExplorer explorer = new TransitionSystemExplorer(this, model, randomSeed);
			explorer.explore();
		}
	}
}
