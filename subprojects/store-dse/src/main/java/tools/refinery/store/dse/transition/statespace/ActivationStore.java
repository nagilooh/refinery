/*
 * SPDX-FileCopyrightText: 2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.dse.transition.statespace;

import tools.refinery.store.tuple.Tuple;

import java.util.Random;

public interface ActivationStore<V> {
	record VisitResult(boolean successfulVisit, boolean mayHaveMore, int transformation, int activation,
					   String transformationName, Tuple activationTuple) {
	}

	// The return value of this method is only useful for exploration strategies that want to synchronise multiple
	// workers and avoid situtation when another worker has already visited the same version.
	@SuppressWarnings("UnusedReturnValue")
	VisitResult markNewAsVisited(V to, int[] emptyEntrySizes);

	boolean hasUnmarkedActivation(V version);

	VisitResult getRandomAndMarkAsVisited(V version, Random random);
}
