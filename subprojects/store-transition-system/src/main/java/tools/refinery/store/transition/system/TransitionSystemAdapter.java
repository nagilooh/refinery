package tools.refinery.store.transition.system;

import tools.refinery.store.transition.system.internal.TransitionSystemBuilderImpl;

public interface TransitionSystemAdapter {

	static TransitionSystemBuilder builder() {
		return new TransitionSystemBuilderImpl();
	}
}
