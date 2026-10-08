/*
 * SPDX-FileCopyrightText: 2023-2026 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.transition.system.strategy;

import tools.refinery.store.transition.system.statespace.State;

public record SubmitResult(boolean include, boolean accepted, State newState) {

	}
