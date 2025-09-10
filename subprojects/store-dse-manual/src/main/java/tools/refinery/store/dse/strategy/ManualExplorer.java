package tools.refinery.store.dse.strategy;

import org.jetbrains.annotations.Nullable;
import tools.refinery.language.semantics.ProblemTrace;
import tools.refinery.store.dse.transition.DesignSpaceExplorationAdapter;
import tools.refinery.store.dse.transition.ObjectiveValue;
import tools.refinery.store.dse.transition.VersionWithObjectiveValue;
import tools.refinery.store.map.Version;
import tools.refinery.store.model.Model;
import tools.refinery.store.query.ModelQueryAdapter;
import tools.refinery.visualization.ModelVisualizerAdapter;
import tools.refinery.visualization.statespace.VisualizationStore;

public class ManualExplorer {
	final ManualStoreManager storeManager;
	final Model model;
	final ProblemTrace problemTrace;
	final DesignSpaceExplorationAdapter explorationAdapter;
	final ManualActivationStoreWorker activationStoreWorker;
	final ModelQueryAdapter queryAdapter;
	private VersionWithObjectiveValue last = null;
	final @Nullable ModelVisualizerAdapter modelVisualizerAdapter;
	final VisualizationStore visualizationStore;
	final boolean isVisualizationEnabled;

	public ManualExplorer(ManualStoreManager storeManager, Model model, ProblemTrace problemTrace) {
		this.storeManager = storeManager;
		this.model = model;
		this.problemTrace = problemTrace;

		explorationAdapter = model.getAdapter(DesignSpaceExplorationAdapter.class);
		queryAdapter = model.getAdapter(ModelQueryAdapter.class);
		activationStoreWorker = new ManualActivationStoreWorker(storeManager.getActivationStore(),
				explorationAdapter.getTransformations(), problemTrace);
		visualizationStore = storeManager.getVisualizationStore();
		modelVisualizerAdapter = model.tryGetAdapter(ModelVisualizerAdapter.class).orElse(null);
		isVisualizationEnabled = modelVisualizerAdapter != null;

		submit();
	}

	public VersionWithObjectiveValue submit() {
		checkSynchronized();
		if (queryAdapter.hasPendingChanges()) {
			throw new AssertionError("Pending changes detected before model submission");
		}
		Version version = model.commit();
		ObjectiveValue objectiveValue = explorationAdapter.getObjectiveValue();
		last = new VersionWithObjectiveValue(version, objectiveValue);
		storeManager.getActivationStore().markNewAsVisited(last, activationStoreWorker.calculateEmptyActivationSize());


		if (isVisualizationEnabled) {
			modelVisualizerAdapter.visualize(last.version());
		}

		return last;
	}

	public boolean manualStep() {
		var newVersion = selectAndVisitUnvisited();
		return newVersion != null;
	}

	public VersionWithObjectiveValue selectAndVisitUnvisited() {
		checkSynchronized();
		if (model.hasUncommittedChanges()) {
			throw new IllegalStateException("The model has uncommitted changes!");
		}

		var visitResult = activationStoreWorker.selectAndFireActivation(this.last);

		if (!visitResult.successfulVisit()) {
			return null;
		}

		queryAdapter.flushChanges();

		Version oldVersion = null;
		if (isVisualizationEnabled) {
			oldVersion = last.version();
		}
		var newVersionWithObjectiveValue = submit();
		if (isVisualizationEnabled && newVersionWithObjectiveValue != null) {
			var newVersion = newVersionWithObjectiveValue.version();
			visualizationStore.addTransition(oldVersion, newVersion,
					"fire: " + visitResult.transformation() + ", " + visitResult.activation());
		}
		return newVersionWithObjectiveValue;
	}

	private void checkSynchronized() {
		if (last != null && !last.version().equals(model.getState())) {
			throw new AssertionError("Worker is not synchronized with model state");
		}
	}
}
