/*
 * SPDX-FileCopyrightText: 2026 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
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
