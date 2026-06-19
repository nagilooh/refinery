/*
 * SPDX-FileCopyrightText: 2023-2024 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.reasoning.actions;

import tools.refinery.logic.AbstractValue;
import tools.refinery.logic.dnf.FunctionalQuery;
import tools.refinery.logic.term.NodeVariable;
import tools.refinery.store.dse.transition.actions.BoundActionLiteral;
import tools.refinery.store.query.resultset.ResultSet;
import tools.refinery.store.reasoning.refinement.PartialInterpretationRefiner;
import tools.refinery.store.reasoning.representation.PartialSymbol;
import tools.refinery.store.tuple.Tuple;

import java.util.List;

public class ComputedJoinActionLiteral<A extends AbstractValue<A, C>, C> extends ComputedPartialActionLiteral<A, C> {

	public ComputedJoinActionLiteral(PartialSymbol<A, C> partialSymbol, List<NodeVariable> parameters,
                                     FunctionalQuery<A> valueQuery, List<NodeVariable> arguments) {
		super(partialSymbol, parameters, valueQuery, arguments);
	}

	@Override
	protected BoundActionLiteral bindToModel(PartialInterpretationRefiner<A, C> refiner, ResultSet<A> resultSet) {
		return tuple -> {
			var value = resultSet.get(tuple.map(argumentMapping));
			if (value == null) {
				return null;
			}
			return refiner.merge(tuple.map(parameterMapping), value) ? Tuple.of() : null;
		};
	}
}
