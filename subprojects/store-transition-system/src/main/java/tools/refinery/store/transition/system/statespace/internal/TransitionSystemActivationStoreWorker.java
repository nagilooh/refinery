/*
 * SPDX-FileCopyrightText: 2023-2026 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.transition.system.statespace.internal;

import tools.refinery.store.dse.transition.statespace.ActivationStore;
import tools.refinery.store.map.Version;
import tools.refinery.store.transition.system.statespace.FiredTransition;
import tools.refinery.store.transition.system.statespace.Transition;

import java.util.List;
import java.util.Random;

public class TransitionSystemActivationStoreWorker {
	final ActivationStore<Version> store;
	final List<Transition> transitions;

	public TransitionSystemActivationStoreWorker(ActivationStore<Version> store, List<Transition> transitions) {
		this.store = store;
		this.transitions = transitions;
	}

	public int[] calculateEmptyActivationSize() {
		int[] result = new int[transitions.size()];
		for (int i = 0; i < result.length; i++) {
			result[i] = transitions.get(i).getAllActivationsAsResultSet().size();
		}
		return result;
	}

	public ActivationStore.VisitResult selectRandomActivation(Version thisVersion, Random random) {
		var result = store.getRandomAndMarkAsVisited(thisVersion, random);
		if (result.successfulVisit()) {
			int selectedTransition = result.transformation();
			int selectedActivation = result.activation();

			var transition = transitions.get(selectedTransition);
			var tuple = transition.getActivation(selectedActivation);

			boolean success = transition.mergePreconditionTrue(tuple);
			if (success) {
				return new ActivationStore.VisitResult(
						true,
						result.mayHaveMore(),
						selectedTransition,
						selectedActivation);
			} else {
				return new ActivationStore.VisitResult(
						false,
						result.mayHaveMore(),
						selectedTransition,
						selectedActivation);
			}
		}
		return result;
	}

	public FiredTransition fireActivation(int selectedTransition, int selectedActivation) {
		var transition = transitions.get(selectedTransition);
		var activation = transition.getActivation(selectedActivation);
		var success = transition.fireAction(activation);
		if (success) {
			return new FiredTransition(transition, activation);
		}
		return null;
	}
}
