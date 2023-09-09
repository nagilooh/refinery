/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.dse.logging;

import tools.refinery.store.dse.transition.VersionWithObjectiveValue;
import tools.refinery.store.map.Version;
import tools.refinery.store.model.ModelStore;

public interface Logger {
	void logState(Version state);
	void logState(Version state, String label);
	void logState(VersionWithObjectiveValue state);
	void logState(VersionWithObjectiveValue state, String label);
	void logSolution(Version state);
	void logSolution(VersionWithObjectiveValue state);
	void logTransition(Version from, Version to);
	void logTransition(Version from, Version to, String label);
	void logTransition(VersionWithObjectiveValue from, VersionWithObjectiveValue to);
	void logTransition(VersionWithObjectiveValue from, VersionWithObjectiveValue to, String label);
	void init(ModelStore store);
	void flush();
}
