package tools.refinery.store.transition.system.simple;

public interface SimpleTransitionSystemAdapter {

	static SimpleTransitionSystemBuilder builder() {
		return new SimpleTransitionSystemBuilderImpl();
	}
}
