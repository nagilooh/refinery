package tools.refinery.store.dse.transition.statespace.internal;

import tools.refinery.store.dse.transition.statespace.StateSpaceAdapter;
import tools.refinery.store.dse.transition.statespace.StateSpaceStoreAdapter;
import tools.refinery.store.model.Model;

public class StateSpaceAdapterImpl implements StateSpaceAdapter {
	final StateSpaceStoreAdapter storeAdapter;
	final Model model;

	StateSpaceAdapterImpl(StateSpaceStoreAdapter storeAdapter, Model model) {
		this.storeAdapter = storeAdapter;
		this.model = model;
	}

	@Override
	public Model getModel() {
		return model;
	}

	@Override
	public StateSpaceStoreAdapter getStoreAdapter() {
		return storeAdapter;
	}
}
