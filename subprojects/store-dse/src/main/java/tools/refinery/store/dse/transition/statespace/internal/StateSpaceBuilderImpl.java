package tools.refinery.store.dse.transition.statespace.internal;

import tools.refinery.store.adapter.AbstractModelAdapterBuilder;
import tools.refinery.store.dse.transition.statespace.ActivationStore;
import tools.refinery.store.dse.transition.statespace.StateSpaceBuilder;
import tools.refinery.store.model.ModelStore;

public class StateSpaceBuilderImpl extends AbstractModelAdapterBuilder<StateSpaceStoreAdapterImpl> implements StateSpaceBuilder {
	private ActivationStore activationStore;

	@Override
	protected StateSpaceStoreAdapterImpl doBuild(ModelStore store) {
		return new StateSpaceStoreAdapterImpl(store);
	}

	@Override
	public StateSpaceBuilder activationStore(ActivationStore activationStore) {
		checkNotConfigured();
		this.activationStore = activationStore;
		return this;
	}
}
