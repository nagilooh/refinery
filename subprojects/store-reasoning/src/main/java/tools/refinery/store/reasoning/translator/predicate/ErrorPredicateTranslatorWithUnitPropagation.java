/*
 * SPDX-FileCopyrightText: 2021-2025 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.reasoning.translator.predicate;

import org.jetbrains.annotations.NotNull;
import tools.refinery.logic.AbstractCall;
import tools.refinery.logic.Constraint;
import tools.refinery.logic.dnf.DnfClause;
import tools.refinery.logic.dnf.Query;
import tools.refinery.logic.dnf.RelationalQuery;
import tools.refinery.logic.literal.*;
import tools.refinery.logic.term.ConstantTerm;
import tools.refinery.logic.term.NodeVariable;
import tools.refinery.logic.term.Variable;
import tools.refinery.logic.term.abstractdomain.*;
import tools.refinery.logic.term.intinterval.IntInterval;
import tools.refinery.logic.term.intinterval.IntIntervalTerms;
import tools.refinery.logic.term.truthvalue.TruthValue;
import tools.refinery.store.dse.propagation.PropagationBuilder;
import tools.refinery.store.dse.transition.Rule;
import tools.refinery.store.model.ModelStoreBuilder;
import tools.refinery.store.reasoning.ReasoningAdapter;
import tools.refinery.store.reasoning.actions.PartialActionLiterals;
import tools.refinery.store.reasoning.literal.*;
import tools.refinery.store.reasoning.representation.PartialRelation;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static tools.refinery.store.reasoning.literal.PartialLiterals.*;

public class ErrorPredicateTranslatorWithUnitPropagation extends PredicateTranslator {

	private final RelationalQuery query;
	private final PartialRelation nodePartialRelation;

	public ErrorPredicateTranslatorWithUnitPropagation(PartialRelation relation, RelationalQuery query,
													   List<PartialRelation> parameterTypes,
													   Set<PartialRelation> supersets, boolean mutable,
													   TruthValue defaultValue, PartialRelation nodePartialRelation) {
		super(relation, query, parameterTypes, supersets, mutable, defaultValue);
		this.query = query;
		this.nodePartialRelation = nodePartialRelation;
	}

	@Override
	public void apply(ModelStoreBuilder storeBuilder) {
		super.apply(storeBuilder);
		storeBuilder.tryGetAdapter(PropagationBuilder.class).ifPresent(this::errorPredicatePropagationRuleTranslator);
	}

	private void errorPredicatePropagationRuleTranslator(@NotNull PropagationBuilder propagationBuilder) {
		List<RuleParameters> propRuleParameters = new ArrayList<>();
		var dnf = query.getDnf();
		for (int clauseIndex = 0; clauseIndex < dnf.getClauses().size(); clauseIndex++) {
			final var clause = dnf.getClauses().get(clauseIndex);

			for (int literalIndex = 0; literalIndex < clause.literals().size(); literalIndex++) {
				final var literalToPropagate = clause.literals().get(literalIndex);
				String propagationName = "#propagateError#" + query.name() +
						"#c" + clauseIndex + "l" + literalIndex;
				RuleParameters propRule = null;
				if (literalToPropagate instanceof CallLiteral callLiteral && shouldPropagate(callLiteral, clause)) {
					propRule = createRuleParameters(callLiteral, propagationName, clause, literalIndex);
				} else if (literalToPropagate instanceof PartialCheckLiteral partialCheckLiteral) {
					propRule = createRuleParameters(partialCheckLiteral, propagationName, clause, literalIndex);
				}
				if (propRule != null) {
					propRuleParameters.add(propRule);
				}

				for (var propRuleParameter : propRuleParameters) {
					var preconditionQuery = Query.of(propagationName + "#precondition", builder -> builder
							.parameters(propRuleParameter.ruleParameters())
							.clause(propRuleParameter.precondition()));

					var rule = Rule.of(propagationName, builder -> builder
							.parameters(propRuleParameter.ruleParameters())
							.clause(must(preconditionQuery.call(CallPolarity.POSITIVE,
									propRuleParameter.ruleParameters())))
							.action(PartialActionLiterals.merge(propRuleParameter.partialRelationTarget(),
									propRuleParameter.toMerge(), propRuleParameter.actionParameters())));
					propagationBuilder.rule(rule);

					var concretizationRule = Rule.of(propagationName + "#concretize", builder -> builder
							.parameters(propRuleParameter.ruleParameters())
							.clause(candidateMust(preconditionQuery.call(CallPolarity.POSITIVE,
									propRuleParameter.ruleParameters())))
							.action(PartialActionLiterals.merge(propRuleParameter.partialRelationTarget(),
									propRuleParameter.toMerge(), propRuleParameter.actionParameters())));
					propagationBuilder.concretizationRule(concretizationRule);
				}
			}
		}
	}

	private RuleParameters createRuleParameters(CallLiteral callLiteral, String propagationName, DnfClause clause,
												int literalIndex) {
		var target = callLiteral.getTarget();
		if (target instanceof PartialRelation partialRelationTarget) {
			var collectedParameters = collectRuleAndActionParameters(callLiteral, clause,
					propagationName);

			List<NodeVariable> ruleParameters = collectedParameters.ruleParameters();
			List<NodeVariable> actionParameters = collectedParameters.actionParameters();

			List<Literal> precondition = collectPrecondition(clause, literalIndex);

			precondition.add(addModality(callLiteral, Modality.MAY));
			precondition.add(Literals.not(addModality(callLiteral, Modality.MUST)));

			// Action = ! literalToPropagate
			final TruthValue toMerge;
			if (callLiteral.getPolarity() == CallPolarity.POSITIVE) {
				toMerge = TruthValue.FALSE;
			} else if (callLiteral.getPolarity() == CallPolarity.NEGATIVE) {
				toMerge = TruthValue.TRUE;
			} else {
				throw new UnsupportedOperationException("I do not know what to do");
			}
			return new RuleParameters(
					ruleParameters,
					precondition,
					partialRelationTarget,
					toMerge,
					actionParameters
			);
		}
		return null;
	}

	@SuppressWarnings("unchecked")
	private RuleParameters createRuleParameters(PartialCheckLiteral partialCheckLiteral, String propagationName,
												DnfClause clause, int literalIndex) {
		var term = partialCheckLiteral.getTerm();
		if (term.getType().equals(TruthValue.class) && term instanceof AbstractDomainBinaryTerm<?, ?, ?> abstractDomainBinaryTerm) {
			TermType termType;
			switch (term) {
			case AbstractDomainLessTerm<?, ?> ignored -> termType = TermType.LESS;
			case AbstractDomainLessEqTerm<?, ?> ignored -> termType = TermType.LESS_EQ;
			case AbstractDomainGreaterTerm<?, ?> ignored -> termType = TermType.GREATER;
			case AbstractDomainGreaterEqTerm<?, ?> ignored -> termType = TermType.GREATER_EQ;
			case AbstractDomainEqTerm<?, ?> ignored -> {
				termType = TermType.EQ;
				return null;
			}
			case AbstractDomainNotEqTerm<?, ?> ignored -> {
				termType = TermType.NOT_EQ;
				return null;
			}
			default -> {
				return null;
			}
			}
			var left = abstractDomainBinaryTerm.getLeft();
			var right = abstractDomainBinaryTerm.getRight();
			PartialCountTerm partialCountTerm;
			ConstantTerm<IntInterval> constantTerm;
			var swapped = false;
			switch (left) {
			case PartialCountTerm pc when right instanceof ConstantTerm<?> ct && ct.getValue() instanceof IntInterval -> {
				partialCountTerm = pc;
				constantTerm = (ConstantTerm<IntInterval>) ct;
			}
			case ConstantTerm<?> ct when ct.getValue() instanceof IntInterval && right instanceof PartialCountTerm pc -> {
				partialCountTerm = pc;
				constantTerm = (ConstantTerm<IntInterval>) ct;
				swapped = true;
			}
			default -> {
				return null;
			}
			}
			if (termType == TermType.LESS_EQ) {
				constantTerm =
						new ConstantTerm<>(IntInterval.class, constantTerm.getValue().add(IntInterval.ONE));
			} else if (termType == TermType.GREATER_EQ) {
				constantTerm = new ConstantTerm<>(IntInterval.class,
						constantTerm.getValue().sub(IntInterval.ONE));
			}
			var target = partialCountTerm.getTarget();
			if (target instanceof PartialRelation partialRelationTarget) {
				var collectedParameters = collectRuleAndActionParameters(partialCountTerm, clause,
						propagationName);

				List<NodeVariable> ruleParameters = collectedParameters.ruleParameters();
				List<NodeVariable> actionParameters = collectedParameters.actionParameters();

				List<Literal> precondition = collectPrecondition(clause, literalIndex);

				Constraint countedConstraint;
				TruthValue toMerge;
				var countModality = calcualteModality(termType, swapped);
				if (countModality == null) {
					return null;
				}
				countedConstraint = ModalConstraint.of(countModality, Concreteness.PARTIAL, partialRelationTarget);
				toMerge = countModality.equals(Modality.MAY) ? TruthValue.TRUE : TruthValue.FALSE;

				var countTerm = new PartialCountTerm(countedConstraint, partialCountTerm.getArguments());
				PartialCheckLiteral countPartialCheckLiteral = new PartialCheckLiteral(IntIntervalTerms.eq(countTerm,
						constantTerm));

				precondition.add(countPartialCheckLiteral);
				CallLiteral callLiteral = new CallLiteral(CallPolarity.POSITIVE,
						partialRelationTarget, new ArrayList<>(actionParameters));
				precondition.add(addModality(callLiteral, Modality.MAY));
				precondition.add(Literals.not(addModality(callLiteral, Modality.MUST)));

				return new RuleParameters(
						ruleParameters,
						precondition,
						partialRelationTarget,
						toMerge,
						actionParameters
				);
			}
		}
		return null;
	}

	private Modality calcualteModality(TermType termType, boolean swapped) {
		if (((termType == TermType.LESS || termType == TermType.LESS_EQ) && !swapped) ||
				((termType == TermType.GREATER || termType == TermType.GREATER_EQ) && swapped)) {
			return Modality.MAY;
		} else if ((termType == TermType.LESS || termType == TermType.LESS_EQ) ||
				(termType == TermType.GREATER || termType == TermType.GREATER_EQ)) {
			return Modality.MUST;
		} else {
			return null;
		}
	}

	// Precondition = []clause - {literalToPropagate} + <>literalToPropagate + ![]literalToPropagate
	private List<Literal> collectPrecondition(DnfClause clause, int literalIndex) {
		List<Literal> precondition = new ArrayList<>();
		for (int i = 0; i < clause.literals().size(); i++) {
			if (i != literalIndex) {
				var lit = clause.literals().get(i);
				switch (lit) {
				case CallLiteral calLit -> {
					precondition.add(calLit);
					for (var arg : calLit.getArguments()) {
						// Add node(arg) constraint
						if (clause.positiveVariables().contains(arg)) {
							precondition.add(nodePartialRelation.call(arg));
						}
					}
				}
				case ConstantLiteral constantLiteral -> precondition.add(constantLiteral);
				case PartialCheckLiteral partialCheckLiteral -> precondition.add(partialCheckLiteral);
				default -> throw new UnsupportedOperationException();
				}
			}
		}
		return precondition;
	}

	private RuleParameters collectRuleAndActionParameters(AbstractCall call, DnfClause clause, String propagationName) {
		List<NodeVariable> ruleParameters = new ArrayList<>();
		List<NodeVariable> actionParameters = new ArrayList<>();
		for (var argument : call.getArguments()) {
			if (argument instanceof NodeVariable nodeVariable) {
				if (!clause.positiveVariables().contains(argument)) {
					var newPositiveVariable = Variable.of(propagationName + "#newPositiveVariable");
					actionParameters.add(newPositiveVariable);
					ruleParameters.add(newPositiveVariable);
				} else {
					actionParameters.add(nodeVariable);
					if (!ruleParameters.contains(nodeVariable)) {
						ruleParameters.add(nodeVariable);
					}
				}
			} else {
				throw new IllegalArgumentException("This argument is illegal");
			}
		}
		return new RuleParameters(ruleParameters, null, null, null, actionParameters);
	}

	private boolean shouldPropagate(CallLiteral literal, DnfClause clause) {
		var target = literal.getTarget();
		if (target.equals(ReasoningAdapter.EQUALS_SYMBOL)) {
			return false;
		}

		if (literal.getPolarity() == CallPolarity.NEGATIVE) {
			for (var arg : literal.getArguments()) {
				if (!clause.positiveVariables().contains(arg)) {
					return false;
				}
			}
		}
		return true;
	}
}
