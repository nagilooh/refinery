package tools.refinery.store.reasoning.translator.predicate;

import org.jetbrains.annotations.NotNull;
import tools.refinery.logic.dnf.RelationalQuery;
import tools.refinery.logic.literal.CallLiteral;
import tools.refinery.logic.literal.CallPolarity;
import tools.refinery.logic.literal.Literal;
import tools.refinery.logic.literal.Literals;
import tools.refinery.logic.term.NodeVariable;
import tools.refinery.logic.term.truthvalue.TruthValue;
import tools.refinery.store.dse.propagation.PropagationBuilder;
import tools.refinery.store.dse.transition.Rule;
import tools.refinery.store.model.ModelStoreBuilder;
import tools.refinery.store.reasoning.ReasoningAdapter;
import tools.refinery.store.reasoning.ReasoningBuilder;
import tools.refinery.store.reasoning.actions.PartialActionLiterals;
import tools.refinery.store.reasoning.representation.PartialRelation;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static tools.refinery.store.reasoning.literal.PartialLiterals.may;
import static tools.refinery.store.reasoning.literal.PartialLiterals.must;

public class ErrorPredicateTranslatorWithUnitPropagation extends PredicateTranslator {

	private final RelationalQuery query;

	public ErrorPredicateTranslatorWithUnitPropagation(PartialRelation relation, RelationalQuery query, List<PartialRelation> parameterTypes, Set<PartialRelation> supersets, boolean mutable, TruthValue defaultValue) {
		super(relation, query, parameterTypes, supersets, mutable, defaultValue);
		this.query = query;
	}

	@Override
	public void apply(ModelStoreBuilder storeBuilder) {
		super.apply(storeBuilder);
		var builder = storeBuilder.getAdapter(ReasoningBuilder.class);
		////builder.

		storeBuilder.tryGetAdapter(PropagationBuilder.class).ifPresent(this::errorPredicatePropagationRuleTranslator);
	}

	private void errorPredicatePropagationRuleTranslator(@NotNull PropagationBuilder propagationBuilder) {
		var dnf = query.getDnf();
		for (int clauseIndex = 0; clauseIndex < dnf.getClauses().size(); clauseIndex++) {
			final var clause = dnf.getClauses().get(clauseIndex);

			for (int literalIndex = 0; literalIndex < clause.literals().size(); literalIndex++) {
				final var literalToPropagate = clause.literals().get(literalIndex);
				if (literalToPropagate instanceof CallLiteral callLiteral) {
					if (!toPropagate(callLiteral)) {
						continue;
					}

					System.out.println(literalToPropagate);

					var target = callLiteral.getTarget();
					if (target instanceof PartialRelation partialRelationTarget) {
						String propagationName = "#propagateError#" + query.name() +
								"#c" + clauseIndex + "l" + literalIndex;

						//literalToPropagate.getInputVariables()
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
								if(lit instanceof CallLiteral calLit) {
									precondition.add(must(calLit));
								} else {
									throw new UnsupportedOperationException();
								}
							}
						}
						precondition.add(may(callLiteral));
						precondition.add(Literals.not(must(callLiteral)));

						// Action = ! literalToPropagate
						final TruthValue toMerge;
						if (callLiteral.getPolarity() == CallPolarity.POSITIVE) {
							toMerge = TruthValue.FALSE;
						} else if (callLiteral.getPolarity() == CallPolarity.NEGATIVE) {
							toMerge = TruthValue.TRUE;
						} else {
							throw new UnsupportedOperationException("I do not know what to do");
						}

						var rule = Rule.of(propagationName, builder -> {
							for (var parameter : parameters) {
								builder.parameter(parameter);
							}
							builder.clause(precondition);
							builder.action(
									PartialActionLiterals.merge(partialRelationTarget, toMerge, parameters)
							);
						});
						propagationBuilder.rule(rule);
					}
				}
			}
		}
	}

	private boolean toPropagate(CallLiteral literal) {
		var target = literal.getTarget();
		if (target.equals(ReasoningAdapter.EQUALS_SYMBOL)) {
			return false;
		}

		if (literal.getPolarity() == CallPolarity.NEGATIVE) {
			for (var arg : literal.getArguments()) {
				if (arg.getName().startsWith("_")) {
					return false;
				}
			}
		}
		return true;
	}
}
