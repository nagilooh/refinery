/*
 * SPDX-FileCopyrightText: 2023-2026 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.transition.system.internal;

import tools.refinery.store.dse.transition.objectives.Criterion;
import tools.refinery.store.dse.transition.objectives.CriterionCalculator;
import tools.refinery.store.model.Model;
import tools.refinery.store.model.ModelStore;
import tools.refinery.store.transition.system.TransitionSystemStoreAdapter;
import tools.refinery.store.transition.system.statespace.Transition;

import java.util.List;

public class TransitionSystemStoreAdapterImpl implements TransitionSystemStoreAdapter {
	protected final ModelStore store;

	protected final List<Transition.Builder> transitions;
	protected final List<Criterion> accepts;

	public TransitionSystemStoreAdapterImpl(ModelStore store, List<Transition.Builder> transitions, List<Criterion> accepts) {
		this.store = store;
		this.transitions = transitions;
		this.accepts = accepts;
	}

	@Override
	public ModelStore getStore() {
		return store;
	}

	@Override
	public TransitionSystemAdapterImpl createModelAdapter(Model model) {
		final List<Transition> t = this.transitions.stream().map(x -> x.build(model)).toList();
		final List<CriterionCalculator> a = this.accepts.stream().map(x -> x.createCalculator(model)).toList();

		return new TransitionSystemAdapterImpl(model, this, t, a);
	}

	@Override
	public List<Transition.Builder> getTransitions() {
		return transitions;
	}

	@Override
	public List<Criterion> getAccepts() {
		return accepts;
	}
}
