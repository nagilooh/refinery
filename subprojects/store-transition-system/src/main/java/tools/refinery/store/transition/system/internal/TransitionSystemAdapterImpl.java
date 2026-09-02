/*
 * SPDX-FileCopyrightText: 2023-2026 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.transition.system.internal;

import tools.refinery.store.dse.transition.objectives.CriterionCalculator;
import tools.refinery.store.model.Model;
import tools.refinery.store.transition.system.TransitionSystemAdapter;
import tools.refinery.store.transition.system.TransitionSystemStoreAdapter;
import tools.refinery.store.transition.system.statespace.Transition;

import java.util.List;

public class TransitionSystemAdapterImpl implements TransitionSystemAdapter {
	final Model model;
	final TransitionSystemStoreAdapter transitionSystemStoreAdapter;

	final List<Transition> transitions;
	final List<CriterionCalculator> accepts;

	public TransitionSystemAdapterImpl(Model model,
									   TransitionSystemStoreAdapter transitionSystemStoreAdapter,
                                       List<Transition> transitions,
                                       List<CriterionCalculator> accepts) {
		this.model = model;
		this.transitionSystemStoreAdapter = transitionSystemStoreAdapter;

		this.transitions = transitions;
		this.accepts = accepts;
	}

	@Override
	public Model getModel() {
		return model;
	}

	@Override
	public TransitionSystemStoreAdapter getStoreAdapter() {
		return transitionSystemStoreAdapter;
	}

	@Override
	public List<Transition> getTransitions() {
		return transitions;
	}

	@Override
	public boolean checkAccept() {
		for (var accept : this.accepts) {
			model.checkCancelled();
			if (!accept.isSatisfied()) {
				return false;
			}
		}
		return true;
	}
}
