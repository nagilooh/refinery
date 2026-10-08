/*
 * SPDX-FileCopyrightText: 2026 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.transition.system.strategy.concretizer;

import tools.refinery.store.transition.system.statespace.Trace;

public abstract class TraceConcretizer {

	protected final Trace traceToConcretize;

	public TraceConcretizer(Trace trace) {
		this.traceToConcretize = trace;
	}

	public abstract Trace concretize();
}
