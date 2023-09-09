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
	void init(ModelStore store);
	void flush();
	default void logState(Version state) {
		logState(state, null);
	}
	void logState(Version state, String label);
	default void logState(VersionWithObjectiveValue state) {
		logState(state, null);
	}
	default void logState(VersionWithObjectiveValue state, String label) {
		logState(state.version(), label);
	}
	void logSolution(Version state);
	default void logSolution(VersionWithObjectiveValue state) {
		logSolution(state.version());
	}
	default void logTransition(Version from, Version to) {
		logTransition(from, to, null);
	}
	void logTransition(Version from, Version to, String label);
	default void logTransition(VersionWithObjectiveValue from, VersionWithObjectiveValue to) {
		logTransition(from, to, null);
	}
	default void logTransition(VersionWithObjectiveValue from, VersionWithObjectiveValue to, String label) {
		logTransition(from.version(), to.version(), label);
	}
}
