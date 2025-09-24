package tools.refinery.store.dse.transition.statespace;

import tools.refinery.store.adapter.ModelAdapter;
import tools.refinery.store.dse.transition.statespace.internal.StateSpaceBuilderImpl;

public interface StateSpaceAdapter extends ModelAdapter {

	StateSpaceStoreAdapter getStoreAdapter();



	static StateSpaceBuilder builder() {
		return new StateSpaceBuilderImpl();
	}
}
