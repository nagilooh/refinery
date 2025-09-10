package tools.refinery.store.dse.strategy;

import tools.refinery.language.semantics.ProblemTrace;
import tools.refinery.store.dse.transition.DesignSpaceExplorationStoreAdapter;
import tools.refinery.store.dse.transition.VersionWithObjectiveValue;
import tools.refinery.store.dse.transition.statespace.ActivationStore;
import tools.refinery.store.dse.transition.statespace.internal.ActivationStoreImpl;
import tools.refinery.store.map.Version;
import tools.refinery.store.model.ModelStore;
import tools.refinery.visualization.statespace.VisualizationStore;
import tools.refinery.visualization.statespace.internal.VisualizationStoreImpl;

import java.util.function.Consumer;

public class ManualStoreManager {
	ModelStore modelStore;
	ActivationStore activationStore;
	VisualizationStore visualizationStore;
	ProblemTrace problemTrace;

	public ManualStoreManager(ModelStore modelStore) {
		this(modelStore, null);
	}

	public ManualStoreManager(ModelStore modelStore, ProblemTrace problemTrace) {
		this.modelStore = modelStore;
		this.problemTrace = problemTrace;
		DesignSpaceExplorationStoreAdapter storeAdapter =
				modelStore.getAdapter(DesignSpaceExplorationStoreAdapter.class);
		Consumer<VersionWithObjectiveValue> whenAllActivationsVisited = x -> {};
		activationStore = new ActivationStoreImpl(storeAdapter.getTransformations(), whenAllActivationsVisited);

		visualizationStore = new VisualizationStoreImpl();
	}

	public ModelStore getModelStore() {
		return modelStore;
	}

	public ActivationStore getActivationStore() {
		return activationStore;
	}

	public VisualizationStore getVisualizationStore() {
		return visualizationStore;
	}

	public void manualStep(Version initial) {
		try (var model = modelStore.createModelForState(initial)) {
			ManualExplorer manualExplorer = new ManualExplorer(this, model, problemTrace);
			var shouldContinue = true;
			while(shouldContinue) {
				shouldContinue = manualExplorer.manualStep();
			}
		}
	}


}
