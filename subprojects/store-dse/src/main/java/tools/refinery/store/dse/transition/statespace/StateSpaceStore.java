/*
 * SPDX-FileCopyrightText: 2026 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.dse.transition.statespace;

import tools.refinery.store.dse.transition.ObjectiveValue;
import tools.refinery.store.map.Version;

import java.util.List;

public interface StateSpaceStore {
	record State(Version version, int id, int stateCode, boolean isSolution, boolean isError, ObjectiveValue objectiveValue) {
	}

	record StateTransition(Version from, Version to, int id, ActivationStore.VisitResult visitResult) {
	}

	default void addState(Version state, int stateCode, boolean isSolution, boolean isError) {
		addState(state, stateCode, isSolution, isError, null);
	}
	void addState(Version state, int stateCode, boolean isSolution, boolean isError, ObjectiveValue objectiveValue);
	void addTransition(Version from, Version to, ActivationStore.VisitResult visitResult);
	void addTransition(Version from, int to, ActivationStore.VisitResult visitResult);
	List<State> getStates();
	List<StateTransition> getTransitions();
	List<StateTransition> getTransitionsToAlreadyVisited();
	int getStateId(Version state);
}
