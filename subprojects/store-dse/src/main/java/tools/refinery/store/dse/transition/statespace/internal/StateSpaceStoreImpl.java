/*
 * SPDX-FileCopyrightText: 2026 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.dse.transition.statespace.internal;

import tools.refinery.store.dse.transition.ObjectiveValue;
import tools.refinery.store.dse.transition.statespace.ActivationStore;
import tools.refinery.store.dse.transition.statespace.StateSpaceStore;
import tools.refinery.store.map.Version;

import java.util.ArrayList;
import java.util.List;

public class StateSpaceStoreImpl implements StateSpaceStore {

	private final List<State> states = new ArrayList<>();
	private final List<StateTransition> transitions = new ArrayList<>();
	private final List<StateTransition> transitionsToAlreadyVisited = new ArrayList<>();

	@Override
	public void addState(Version state, int stateCode, boolean isSolution, boolean isError, ObjectiveValue objectiveValue) {
		states.add(new State(state, states.size(), stateCode, isSolution, isError, objectiveValue));
	}

	@Override
	public void addTransition(Version from, Version to, ActivationStore.VisitResult visitResult) {
		transitions.add(new StateTransition(from, to, transitions.size(), visitResult));
	}

	@Override
	public void addTransition(Version from, int toStateCode, ActivationStore.VisitResult visitResult) {
		var toState = states.stream().filter(s -> s.stateCode() == toStateCode).findFirst();
		if (toState.isPresent()) {
			transitionsToAlreadyVisited.add(new StateTransition(from, toState.get().version(), transitions.size(), visitResult));
		} else {
			throw new IllegalArgumentException("No state with stateCode " + toStateCode + " found.");
		}
	}

	@Override
	public List<State> getStates() {
		return states;
	}

	@Override
	public List<StateTransition> getTransitions() {
		return transitions;
	}

	@Override
	public List<StateTransition> getTransitionsToAlreadyVisited() {
		return transitionsToAlreadyVisited;
	}

	@Override
	public int getStateId(Version state) {
		return states.stream()
				.filter(s -> s.version().equals(state))
				.map(State::id)
				.findFirst()
				.orElseThrow(() -> new IllegalArgumentException("State not found: " + state));
	}


}
