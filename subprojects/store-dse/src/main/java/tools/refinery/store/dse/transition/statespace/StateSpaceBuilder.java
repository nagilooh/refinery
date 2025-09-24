package tools.refinery.store.dse.transition.statespace;

import tools.refinery.store.adapter.ModelAdapterBuilder;

public interface StateSpaceBuilder extends ModelAdapterBuilder {

	StateSpaceBuilder activationStore(ActivationStore activationStore);
}
