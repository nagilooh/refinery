/*
 * SPDX-FileCopyrightText: 2021-2025 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.logic.literal;

import tools.refinery.logic.AbstractCall;
import tools.refinery.logic.Constraint;
import tools.refinery.logic.equality.LiteralEqualityHelper;
import tools.refinery.logic.equality.LiteralHashCodeHelper;
import tools.refinery.logic.substitution.Substitution;
import tools.refinery.logic.term.ParameterDirection;
import tools.refinery.logic.term.Variable;
import tools.refinery.logic.util.CallSite;

import java.util.*;

// {@link Object#equals(Object)} is implemented by {@link AbstractLiteral}.
@SuppressWarnings("squid:S2160")
public abstract class AbstractCallLiteral extends AbstractLiteral implements AbstractCall {
	private final CallSite callSite;

	protected AbstractCallLiteral(Constraint target, List<Variable> arguments) {
		callSite = new CallSite(target, arguments);
	}

	@Override
	public Constraint getTarget() {
		return callSite.getTarget();
	}

	@Override
	public List<Variable> getArguments() {
		return callSite.getArguments();
	}

	@Override
	public Set<Variable> getArgumentsOfDirection(ParameterDirection direction) {
		return callSite.getArgumentsOfDirection(direction);
	}

	@Override
	public Set<Variable> getInputVariables(Set<? extends Variable> positiveVariablesInClause) {
		return callSite.getInputVariablesForNonEnumerableCall(positiveVariablesInClause);
	}

	@Override
	public Set<Variable> getPrivateVariables(Set<? extends Variable> positiveVariablesInClause) {
		return callSite.getPrivateVariablesForNonEnumerableCall(positiveVariablesInClause);
	}

	@Override
	public Literal substitute(Substitution substitution) {
		var substitutedArguments = callSite.getSubstitutedArguments(substitution);
		return doSubstitute(substitution, substitutedArguments);
	}

	protected abstract Literal doSubstitute(Substitution substitution, List<Variable> substitutedArguments);

	public AbstractCallLiteral withTarget(Constraint newTarget) {
		if (Objects.equals(getTarget(), newTarget)) {
			return this;
		}
		return withArguments(newTarget, getArguments());
	}

	public abstract AbstractCallLiteral withArguments(Constraint newTarget, List<Variable> newArguments);

	@Override
	public boolean equalsWithSubstitution(LiteralEqualityHelper helper, Literal other) {
		if (!super.equalsWithSubstitution(helper, other)) {
			return false;
		}
		var otherCallLiteral = (AbstractCallLiteral) other;
		return callSite.equalsWithSubstitution(helper, otherCallLiteral.callSite);
	}

	@Override
	public int hashCodeWithSubstitution(LiteralHashCodeHelper helper) {
		return super.hashCodeWithSubstitution(helper) * 31 + callSite.hashCodeWithSubstitution(helper);
	}
}
