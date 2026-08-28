/*
 * SPDX-FileCopyrightText: 2026 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.transition.system.simple;

public interface SimpleTransitionSystemAdapter {

	static SimpleTransitionSystemBuilder builder() {
		return new SimpleTransitionSystemBuilderImpl();
	}
}
