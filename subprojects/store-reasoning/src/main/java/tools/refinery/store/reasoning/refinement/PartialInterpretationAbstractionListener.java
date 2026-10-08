/*
 * SPDX-FileCopyrightText: 2026 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.reasoning.refinement;

import tools.refinery.logic.AbstractValue;
import tools.refinery.store.tuple.Tuple;

@FunctionalInterface
public interface PartialInterpretationAbstractionListener<A extends AbstractValue<A, C>, C> {

	boolean join(Tuple key, A fromValue, A toValue);
}
