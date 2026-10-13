/*
 * SPDX-FileCopyrightText: 2026 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.reasoning.actions;

import tools.refinery.logic.term.NodeVariable;
import tools.refinery.logic.term.Variable;
import tools.refinery.store.dse.transition.actions.AbstractActionLiteral;
import tools.refinery.store.dse.transition.actions.BoundActionLiteral;
import tools.refinery.store.model.Model;
import tools.refinery.store.reasoning.ReasoningAdapter;
import tools.refinery.store.tuple.Tuple;

import java.util.List;

public class CleanupActionLiteral extends AbstractActionLiteral {
	private final NodeVariable variable;
	private final boolean force;

	public CleanupActionLiteral(NodeVariable variable) {
		this(variable, false);
	}

	public CleanupActionLiteral(NodeVariable variable, boolean force) {
		this.variable = variable;
		this.force = force;
	}

	public Variable getVariable() {
		return variable;
	}

	@Override
	public List<NodeVariable> getInputVariables() {
		return List.of(variable);
	}

	@Override
	public List<NodeVariable> getOutputVariables() {
		return List.of();
	}

	@Override
	public BoundActionLiteral bindToModel(Model model) {
		var reasoningAdapter = model.getAdapter(ReasoningAdapter.class);
		return tuple -> {
			reasoningAdapter.cleanup(tuple.get(0), force);
			return Tuple.of();
		};
	}
}
