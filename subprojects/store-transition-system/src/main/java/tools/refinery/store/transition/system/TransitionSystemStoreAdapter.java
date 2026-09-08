/*
 * SPDX-FileCopyrightText: 2023-2026 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.transition.system;

import tools.refinery.store.adapter.ModelStoreAdapter;
import tools.refinery.store.dse.transition.DecisionRule;
import tools.refinery.store.dse.transition.DesignSpaceExplorationAdapter;
import tools.refinery.store.dse.transition.Rule;
import tools.refinery.store.dse.transition.objectives.Criterion;
import tools.refinery.store.model.Model;
import tools.refinery.store.transition.system.statespace.Transition;

import java.util.List;

public interface TransitionSystemStoreAdapter extends ModelStoreAdapter {
	@Override
	TransitionSystemAdapter createModelAdapter(Model model);

	List<Transition.Builder> getTransitions();

	List<Criterion> getAccepts();
}
