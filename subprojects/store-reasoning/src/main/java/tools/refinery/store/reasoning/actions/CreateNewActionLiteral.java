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

import java.util.List;

public class CreateNewActionLiteral extends AbstractActionLiteral {
	private final NodeVariable variable;

	public CreateNewActionLiteral(NodeVariable variable) {
		this.variable = variable;
	}

	public Variable getVariable() {
		return variable;
	}

	@Override
	public List<NodeVariable> getInputVariables() {
		return List.of();
	}

	@Override
	public List<NodeVariable> getOutputVariables() {
		return List.of(variable);
	}

	@Override
	public BoundActionLiteral bindToModel(Model model) {
		var reasoningAdapter = model.getAdapter(ReasoningAdapter.class);
		return _ -> reasoningAdapter.createNew();
	}
}
