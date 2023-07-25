/*
 * SPDX-FileCopyrightText: 2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.reasoning.literal;

import tools.refinery.store.query.Constraint;
import tools.refinery.store.query.literal.AbstractCallLiteral;
import tools.refinery.store.query.literal.Literal;
import tools.refinery.store.query.substitution.Substitution;
import tools.refinery.store.query.term.DataVariable;
import tools.refinery.store.query.term.Variable;
import tools.refinery.store.representation.cardinality.UpperCardinalities;
import tools.refinery.store.representation.cardinality.UpperCardinality;

import java.util.List;

public class CountUpperBoundLiteral extends ConcreteCountLiteral<UpperCardinality> {
	public CountUpperBoundLiteral(DataVariable<UpperCardinality> resultVariable, Concreteness concreteness,
								  Constraint target, List<Variable> arguments) {
		super(UpperCardinality.class, resultVariable, concreteness, target, arguments);
	}

	@Override
	protected UpperCardinality zero() {
		return UpperCardinalities.ZERO;
	}

	@Override
	protected UpperCardinality one() {
		return UpperCardinalities.UNBOUNDED;
	}

	@Override
	protected Literal doSubstitute(Substitution substitution, List<Variable> substitutedArguments) {
		return new CountUpperBoundLiteral(substitution.getTypeSafeSubstitute(getResultVariable()), getConcreteness(),
				getTarget(), substitutedArguments);
	}

	@Override
	protected AbstractCallLiteral internalWithTarget(Constraint newTarget) {
		return new CountUpperBoundLiteral(getResultVariable(), getConcreteness(), newTarget, getArguments());
	}

	@Override
	protected String operatorName() {
		return "@UpperBound(\"%s\") count".formatted(getConcreteness());
	}
}
