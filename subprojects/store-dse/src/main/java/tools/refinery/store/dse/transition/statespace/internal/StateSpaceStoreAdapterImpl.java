package tools.refinery.store.dse.transition.statespace.internal;

import tools.refinery.store.adapter.ModelAdapter;
import tools.refinery.store.dse.transition.statespace.StateSpaceStoreAdapter;
import tools.refinery.store.model.Model;
import tools.refinery.store.model.ModelStore;

public class StateSpaceStoreAdapterImpl implements StateSpaceStoreAdapter {
	ModelStore store;

	StateSpaceStoreAdapterImpl(ModelStore store) {
		this.store = store;
	}

	@Override
	public ModelStore getStore() {
		return store;
	}

	@Override
	public ModelAdapter createModelAdapter(Model model) {
		return new StateSpaceAdapterImpl(this, model);
	}
}
