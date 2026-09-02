/*
 * SPDX-FileCopyrightText: 2026 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.transition.system.statespace;

import tools.refinery.store.map.Version;

public record State(FiredTransition incomingTransition, Version version, State parent, int depth) {
}
