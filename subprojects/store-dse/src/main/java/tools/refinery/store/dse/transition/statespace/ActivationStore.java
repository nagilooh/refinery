/*
 * SPDX-FileCopyrightText: 2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.dse.transition.statespace;

import tools.refinery.store.dse.transition.Transformation;
import tools.refinery.store.dse.transition.VersionWithObjectiveValue;
import tools.refinery.store.tuple.Tuple;

import java.util.Random;

public interface ActivationStore {
	record VisitResult(boolean successfulVisit, boolean mayHaveMore, int transformation, int activation) { }
	record FireResult(boolean successfulVisit, boolean mayHaveMore, Transformation transformation, Tuple activation) { }
	VisitResult markNewAsVisited(VersionWithObjectiveValue to, int[] emptyEntrySizes);
	boolean hasUnmarkedActivation(VersionWithObjectiveValue version);
	VisitResult getRandomAndMarkAsVisited(VersionWithObjectiveValue version, Random random);
}
