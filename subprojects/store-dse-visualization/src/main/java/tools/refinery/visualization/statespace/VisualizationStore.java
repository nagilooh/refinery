/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.visualization.statespace;

import tools.refinery.store.map.Version;

import java.util.Map;

public interface VisualizationStore {
	void addState(Version state, String label, Integer stateCode);
	void addSolution(Version state);
	void addTransition(Version from, Version to, String label);
	void addTransition(Version from, int to, String label);
	StringBuilder getDesignSpaceStringBuilder(boolean includeTransitionsToAlreadyVisitedStates);
	Map<Version, Integer> getStates();
}
