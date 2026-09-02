/*
 * SPDX-FileCopyrightText: 2026 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.transition.system.statespace;

import tools.refinery.store.dse.transition.Rule;
import tools.refinery.store.reasoning.representation.PartialRelation;

public record TransitionRule(PartialRelation preconditionRelation, Rule rule) {

	public TransitionRule(PartialRelation preconditionRelation, Rule rule) {
		this.preconditionRelation = preconditionRelation;
		this.rule = rule;

		if (preconditionRelation.arity() != rule.getPrecondition().arity()) {
			throw new IllegalArgumentException("Precondition relation and rule precondition must have the same arity");
		}
	}
}
