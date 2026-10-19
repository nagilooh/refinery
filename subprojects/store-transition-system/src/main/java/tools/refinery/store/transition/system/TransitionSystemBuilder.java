/*
 * SPDX-FileCopyrightText: 2026 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.transition.system;

import tools.refinery.store.adapter.ModelAdapterBuilder;
import tools.refinery.store.dse.transition.objectives.Criterion;
import tools.refinery.store.dse.transition.statespace.StateSpaceStore;
import tools.refinery.store.transition.system.statespace.TransitionRule;

import java.util.Collection;
import java.util.List;

public interface TransitionSystemBuilder extends ModelAdapterBuilder {

	default TransitionSystemBuilder transitions(TransitionRule... rules) {
		return transitions(List.of(rules));
	}

	default TransitionSystemBuilder transitions(Collection<TransitionRule> rules) {
		rules.forEach(this::transition);
		return this;
	}

	TransitionSystemBuilder transition(TransitionRule rule);

	TransitionSystemBuilder accept(Criterion criterion);

	TransitionSystemBuilder with(StateSpaceStore stateSpaceStore);
}
