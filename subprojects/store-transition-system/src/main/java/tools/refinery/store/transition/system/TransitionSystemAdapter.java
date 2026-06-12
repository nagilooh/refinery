package tools.refinery.store.transition.system;

import tools.refinery.store.adapter.ModelAdapter;
import tools.refinery.store.transition.system.internal.TransitionSystemBuilderImpl;
import tools.refinery.store.transition.system.statespace.Transition;

import java.util.List;

public interface TransitionSystemAdapter extends ModelAdapter {
	@Override
	TransitionSystemStoreAdapter getStoreAdapter();

	static TransitionSystemBuilder builder() {
		return new TransitionSystemBuilderImpl();
	}

	List<Transition> getTransitions();

	boolean checkAccept();
}
