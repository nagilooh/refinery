package tools.refinery.store.dse.strategy;

import tools.refinery.store.dse.transition.DesignSpaceExplorationStoreAdapter;
import tools.refinery.store.dse.transition.VersionWithObjectiveValue;
import tools.refinery.store.dse.transition.statespace.ActivationStore;
import tools.refinery.store.dse.transition.statespace.EquivalenceClassStore;
import tools.refinery.store.dse.transition.statespace.internal.*;
import tools.refinery.store.map.Version;
import tools.refinery.store.model.Model;
import tools.refinery.store.model.ModelStore;
import tools.refinery.store.statecoding.StateCoderStoreAdapter;

import java.util.function.Consumer;

public class ManualStoreManager {
	ModelStore modelStore;
	ActivationStore activationStore;
	EquivalenceClassStore equivalenceClassStore;
	ManualExplorer manualExplorer;
	Model model;

	public ManualStoreManager(ModelStore modelStore) {
		this.modelStore = modelStore;
		DesignSpaceExplorationStoreAdapter storeAdapter =
				modelStore.getAdapter(DesignSpaceExplorationStoreAdapter.class);

		Consumer<VersionWithObjectiveValue> whenAllActivationsVisited = x -> {};
		activationStore = new CompleteActivationStoreImpl(storeAdapter.getTransformations(), whenAllActivationsVisited);
		equivalenceClassStore = new FastEquivalenceClassStore(modelStore.getAdapter(StateCoderStoreAdapter.class)) {
			@Override
			protected void delegate(VersionWithObjectiveValue version, int[] emptyActivations, boolean accept) {
				throw new UnsupportedOperationException("This equivalence storage is not prepared to resolve " +
						"symmetries!");
			}
		};
	}

	public ModelStore getModelStore() {
		return modelStore;
	}

	ActivationStore getActivationStore() {
		return activationStore;
	}

	EquivalenceClassStore getEquivalenceClassStore() {
		return equivalenceClassStore;
	}

	public void startExploration(Version initial) {
		model = modelStore.createModelForState(initial);
		manualExplorer = new ManualExplorer(this, model);
	}

	public ActivationStore.VisitResult step() {
		return manualExplorer.step();
	}

	public Version getLast() {
		return manualExplorer.last.version();
	}

	public ManualExplorer getManualExplorer() {
		return manualExplorer;
	}

}
