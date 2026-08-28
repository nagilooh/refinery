/*
 * SPDX-FileCopyrightText: 2026 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.transition.system.simple;

import tools.refinery.store.dse.transition.DesignSpaceExplorationBuilder;
import tools.refinery.store.dse.transition.Rule;

public interface SimpleTransitionSystemBuilder extends DesignSpaceExplorationBuilder {

	SimpleTransitionSystemBuilder transition(Rule rule);
}
