package tools.refinery.generator.impl;

import tools.refinery.generator.ModelExplorer;
import tools.refinery.store.dse.strategy.ManualStoreManager;
import tools.refinery.store.dse.transition.statespace.ActivationStore;
import tools.refinery.store.map.Version;
import tools.refinery.store.reasoning.literal.Concreteness;

public class ManualExplorationImpl extends ModelSemanticsImpl implements ModelExplorer {
	private final ManualStoreManager manualStoreManager;

	public ManualExplorationImpl(Args args) {
		super(args);
		manualStoreManager = new ManualStoreManager(getModelStore());
		manualStoreManager.startExploration(getModel().commit());
	}

	@Override
	public Concreteness getConcreteness() {
		return Concreteness.PARTIAL;
	}

	@Override
	public ActivationStore.VisitResult step() {
		return manualStoreManager.step();
	}

	@Override
	public Version getLast() {
		return manualStoreManager.getLast();
	}

	@Override
	public ManualStoreManager getStoreManager() {
		return manualStoreManager;
	}
}
