package tools.refinery.store.reasoning.translator.predicate;

import org.jetbrains.annotations.NotNull;
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
		var dnf = query.getDnf();
		for (int clauseIndex = 0; clauseIndex < dnf.getClauses().size(); clauseIndex++) {
			final var clause = dnf.getClauses().get(clauseIndex);

			for (int literalIndex = 0; literalIndex < clause.literals().size(); literalIndex++) {
				final var literalToPropagate = clause.literals().get(literalIndex);
				if (literalToPropagate instanceof CallLiteral callLiteral) {
					if (!toPropagate(callLiteral, clause)) {
						continue;
					}

					var target = callLiteral.getTarget();
					if (target instanceof PartialRelation partialRelationTarget) {
						String propagationName = "#propagateError#" + query.name() +
								"#c" + clauseIndex + "l" + literalIndex;

						List<NodeVariable> ruleParameters = new ArrayList<>();
						List<NodeVariable> actionParameters = new ArrayList<>();
						for (var argument : callLiteral.getArguments()) {
							if (argument instanceof NodeVariable nodeVariable) {
								actionParameters.add(nodeVariable);
								if (!ruleParameters.contains(nodeVariable)) {
									ruleParameters.add(nodeVariable);
								}
							} else {
								throw new IllegalArgumentException("This argument is illegal");
							}
						}

						// Precondition = []clause - {literalToPropagate} + <>literalToPropagate + ![]literalToPropagate
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

						var preconditionQuery = Query.of(propagationName + "#precondition", builder -> builder
								.parameters(ruleParameters)
								.clause(precondition));

						var rule = Rule.of(propagationName, builder -> builder
								.parameters(ruleParameters)
								.clause(must(preconditionQuery.call(CallPolarity.POSITIVE, ruleParameters)))
								.action(PartialActionLiterals.merge(partialRelationTarget, toMerge, actionParameters)));
						propagationBuilder.rule(rule);

						var concretizationRule = Rule.of(propagationName + "#concretize", builder -> builder
								.parameters(ruleParameters)
								.clause(candidateMust(preconditionQuery.call(CallPolarity.POSITIVE, ruleParameters)))
								.action(PartialActionLiterals.merge(partialRelationTarget, toMerge, actionParameters)));
						propagationBuilder.concretizationRule(concretizationRule);
					}
				} else if (literalToPropagate instanceof PartialCheckLiteral partialCheckLiteral) {
					var term = partialCheckLiteral.getTerm();
					if(term.getType().equals(TruthValue.class) && term instanceof AbstractDomainBinaryTerm abstractDomainBinaryTerm) {
						TermType termType;
						switch (term) {
						case AbstractDomainLessTerm abstractDomainLessTerm -> {
							termType = TermType.LESS;
						}
						case AbstractDomainLessEqTerm abstractDomainLessEqTerm -> {
							termType = TermType.LESS_EQ;
							continue;
						}
						case AbstractDomainGreaterTerm abstractDomainGreaterTerm -> {
							termType = TermType.GREATER;
						}
						case AbstractDomainGreaterEqTerm abstractDomainGreaterEqTerm -> {
							termType = TermType.GREATER_EQ;
							continue;
						}
						case AbstractDomainEqTerm  abstractDomainEqTerm -> {
							termType = TermType.EQ;
							continue;
						}
						case AbstractDomainNotEqTerm abstractDomainNotEqTerm -> {
							termType = TermType.NOT_EQ;
							continue;
						}
						default -> {continue;}
						}
						var left = abstractDomainBinaryTerm.getLeft();
						var right = abstractDomainBinaryTerm.getRight();
						PartialCountTerm partialCountTerm;
						ConstantTerm<IntInterval> constantTerm;
						var swapped = false;
						if (left instanceof PartialCountTerm pc && right instanceof ConstantTerm ct && ct.getType().equals(IntInterval.class)) {
							partialCountTerm = pc;
							constantTerm = (ConstantTerm<IntInterval>) ct;
						} else if (left instanceof ConstantTerm ct && ct.getType().equals(IntInterval.class) && right instanceof PartialCountTerm pc) {
							partialCountTerm = pc;
							constantTerm = (ConstantTerm<IntInterval>) ct;
							swapped = true;
						} else {
							continue;
						}
						var target = partialCountTerm.getTarget();
						if (target instanceof PartialRelation partialRelationTarget) {
							String propagationName = "#propagateError#" + query.name() +
									"#c" + clauseIndex + "l" + literalIndex;

							List<NodeVariable> ruleParameters = new ArrayList<>();
							List<NodeVariable> actionParameters = new ArrayList<>();

							for (var argument : partialCountTerm.getArguments()) {
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

							// Precondition = []clause - {literalToPropagate} + <>literalToPropagate + ![]literalToPropagate
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
									case PartialCheckLiteral partialCheckLiteralCall ->
											precondition.add(partialCheckLiteralCall);
									default -> throw new UnsupportedOperationException();
									}
								}
							}


							Constraint countedConstraint;
							final TruthValue toMerge;
							if (termType == TermType.LESS && !swapped) {
								countedConstraint = ModalConstraint.of(Modality.MAY, Concreteness.PARTIAL,
										partialRelationTarget);
								toMerge = TruthValue.TRUE;
							} else if (termType == TermType.LESS) {
								countedConstraint = ModalConstraint.of(Modality.MUST, Concreteness.PARTIAL,
										partialRelationTarget);
								toMerge = TruthValue.FALSE;
							} else if (termType == TermType.GREATER && !swapped) {
								countedConstraint = ModalConstraint.of(Modality.MUST, Concreteness.PARTIAL,
										partialRelationTarget);
								toMerge = TruthValue.FALSE;
							} else if (termType == TermType.GREATER) {
								countedConstraint = ModalConstraint.of(Modality.MAY, Concreteness.PARTIAL,
										partialRelationTarget);
								toMerge = TruthValue.TRUE;
							} else {
								continue;
							}

							var countTerm = new PartialCountTerm(countedConstraint, partialCountTerm.getArguments());
							PartialCheckLiteral countPartialCheckLiteral = new PartialCheckLiteral(IntIntervalTerms.eq(countTerm,
									constantTerm));

							precondition.add(countPartialCheckLiteral);


							CallLiteral callLiteral = new CallLiteral(CallPolarity.POSITIVE,
									partialRelationTarget, new ArrayList<>(actionParameters));
							precondition.add(addModality(callLiteral, Modality.MAY));
							precondition.add(Literals.not(addModality(callLiteral, Modality.MUST)));

							var preconditionQuery = Query.of(propagationName + "#precondition", builder -> builder
									.parameters(ruleParameters)
									.clause(precondition));

							var rule = Rule.of(propagationName, builder -> builder
									.parameters(ruleParameters)
									.clause(must(preconditionQuery.call(CallPolarity.POSITIVE, ruleParameters)))
									.action(PartialActionLiterals.merge(partialRelationTarget, toMerge,
											actionParameters)));
							propagationBuilder.rule(rule);

							var concretizationRule = Rule.of(propagationName + "#concretize", builder -> builder
									.parameters(ruleParameters)
									.clause(candidateMust(preconditionQuery.call(CallPolarity.POSITIVE, ruleParameters)))
									.action(PartialActionLiterals.merge(partialRelationTarget, toMerge, actionParameters)));
							propagationBuilder.concretizationRule(concretizationRule);
						}
					}
				}
			}
		}
	}

	private boolean toPropagate(CallLiteral literal, DnfClause clause) {
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
