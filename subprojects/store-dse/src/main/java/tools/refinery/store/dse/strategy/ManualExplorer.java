package tools.refinery.store.dse.strategy;

import org.jetbrains.annotations.Nullable;
import tools.refinery.store.dse.propagation.PropagationAdapter;
import tools.refinery.store.dse.transition.DesignSpaceExplorationAdapter;
import tools.refinery.store.dse.transition.ObjectiveValue;
import tools.refinery.store.dse.transition.VersionWithObjectiveValue;
import tools.refinery.store.dse.transition.statespace.ActivationStore;
import tools.refinery.store.dse.transition.statespace.internal.ActivationStoreWorker;
import tools.refinery.store.dse.transition.statespace.internal.ManualActivationStoreWorker;
import tools.refinery.store.map.Version;
import tools.refinery.store.model.Model;
import tools.refinery.store.query.ModelQueryAdapter;
import tools.refinery.store.statecoding.StateCoderAdapter;

public class ManualExplorer {
	final ManualStoreManager storeManager;
	final Model model;
	final ManualActivationStoreWorker activationStoreWorker;
	final DesignSpaceExplorationAdapter explorationAdapter;
	final StateCoderAdapter stateCoderAdapter;
	final ModelQueryAdapter queryAdapter;
	final @Nullable PropagationAdapter propagationAdapter;
	VersionWithObjectiveValue last;

	public ManualExplorer(ManualStoreManager storeManager, Model model) {
		this.storeManager = storeManager;
		this.model = model;

		explorationAdapter = model.getAdapter(DesignSpaceExplorationAdapter.class);
		activationStoreWorker = new ManualActivationStoreWorker(storeManager.getActivationStore(),
				explorationAdapter.getTransformations());

		stateCoderAdapter = model.getAdapter(StateCoderAdapter.class);
		queryAdapter = model.getAdapter(ModelQueryAdapter.class);
		propagationAdapter = model.tryGetAdapter(PropagationAdapter.class).orElse(null);
		last = submit().newVersion();
	}

	private SubmitResult submit() {
		checkSynchronized();
		if (queryAdapter.hasPendingChanges()) {
			throw new AssertionError("Pending changes detected before model submission");
		}

//		var code = stateCoderAdapter.calculateStateCode();
//		boolean isNew = storeManager.getEquivalenceClassStore().submit(code);
		return submitNew();
//		return new SubmitResult(false, false, null, null);
	}

	private SubmitResult submitNew() {
		Version version = model.commit();
		ObjectiveValue objectiveValue = ObjectiveValue.of(1);
		var versionWithObjectiveValue = new VersionWithObjectiveValue(version, objectiveValue);
		var accepted = explorationAdapter.checkAccept();

		storeManager.getActivationStore().markNewAsVisited(versionWithObjectiveValue, activationStoreWorker.calculateEmptyActivationSize());


		return new SubmitResult(true, accepted, objectiveValue, versionWithObjectiveValue);
	}

	public ActivationStore.VisitResult step() {
	// work in progress
		var result = activationStoreWorker.selectAndFireActivation(last);
		if (propagationAdapter != null) {
			var propagationResult = propagationAdapter.propagate();
			if (propagationResult.isRejected()) {
				return new ActivationStore.VisitResult(false, result.mayHaveMore(), result.transformation(), result.activation());
			}
		}
		queryAdapter.flushChanges();
		last = submit().newVersion();
		return result;
	}

	private void checkSynchronized() {
		if (last != null && !last.version().equals(model.getState())) {
			throw new AssertionError("Worker is not synchronized with model state");
		}
	}

	public ManualActivationStoreWorker getActivationStoreWorker() {
		return activationStoreWorker;
	}
}
