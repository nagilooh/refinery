package tools.refinery.store.reasoning.translator.predicate;

import org.jetbrains.annotations.NotNull;
import tools.refinery.logic.dnf.DnfClause;
import tools.refinery.logic.dnf.Query;
import tools.refinery.logic.dnf.RelationalQuery;
import tools.refinery.logic.literal.*;
import tools.refinery.logic.term.NodeVariable;
import tools.refinery.logic.term.truthvalue.TruthValue;
import tools.refinery.store.dse.propagation.PropagationBuilder;
import tools.refinery.store.dse.transition.Rule;
import tools.refinery.store.model.ModelStoreBuilder;
import tools.refinery.store.reasoning.ReasoningAdapter;
import tools.refinery.store.reasoning.actions.PartialActionLiterals;
import tools.refinery.store.reasoning.literal.Modality;
import tools.refinery.store.reasoning.literal.PartialCheckLiteral;
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

						List<NodeVariable> parameters = new ArrayList<>();
						for (var argument : callLiteral.getArguments()) {
							if (argument instanceof NodeVariable nodeVariable) {
								parameters.add(nodeVariable);
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
								.parameters(parameters)
								.clause(precondition));

						var rule = Rule.of(propagationName, builder -> builder
								.parameters(parameters)
								.clause(must(preconditionQuery.call(CallPolarity.POSITIVE, parameters)))
								.action(PartialActionLiterals.merge(partialRelationTarget, toMerge, parameters)));
						propagationBuilder.rule(rule);

						var concretizationRule = Rule.of(propagationName + "#concretize", builder -> builder
								.parameters(parameters)
								.clause(candidateMust(preconditionQuery.call(CallPolarity.POSITIVE, parameters)))
								.action(PartialActionLiterals.merge(partialRelationTarget, toMerge, parameters)));
						propagationBuilder.concretizationRule(concretizationRule);
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
