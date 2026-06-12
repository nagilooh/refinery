package tools.refinery.store.transition.system.simple;

import tools.refinery.logic.dnf.Dnf;
import tools.refinery.logic.literal.CallLiteral;
import tools.refinery.logic.literal.ConstantLiteral;
import tools.refinery.logic.term.NodeVariable;
import tools.refinery.logic.term.truthvalue.TruthValue;
import tools.refinery.store.dse.transition.DecisionRule;
import tools.refinery.store.dse.transition.Rule;
import tools.refinery.store.dse.transition.actions.Action;
import tools.refinery.store.dse.transition.actions.ActionLiteral;
import tools.refinery.store.dse.transition.internal.DesignSpaceExplorationBuilderImpl;
import tools.refinery.store.reasoning.actions.MergeActionLiteral;
import tools.refinery.store.reasoning.lifting.DnfLifter;
import tools.refinery.store.reasoning.literal.Concreteness;
import tools.refinery.store.reasoning.literal.Modality;
import tools.refinery.store.reasoning.representation.PartialRelation;
import tools.refinery.store.reasoning.representation.PartialSymbol;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static tools.refinery.store.reasoning.ReasoningAdapter.EXISTS_SYMBOL;

public class SimpleTransitionSystemBuilderImpl extends DesignSpaceExplorationBuilderImpl implements SimpleTransitionSystemBuilder {

	private final DnfLifter lifter = new DnfLifter();

	@Override
	public SimpleTransitionSystemBuilder transition(Rule rule) {
		var mayPrecondition = lifter.lift(Modality.MAY, Concreteness.PARTIAL, rule.getPrecondition());

		var action = rule.getAction();
		var originalActionLiterals = action.getActionLiterals();
		var dnf = rule.getPrecondition().getDnf();
		var parameters = action.getParameters();
		var conjuncts = singleClauseDnfToConjuncts(dnf, dnfParametersToArguments(dnf, parameters));
		var actionLiterals =
				new ArrayList<ActionLiteral>(parameters.size() + conjuncts.size() + originalActionLiterals.size());

		for (var parameter : parameters) {
			actionLiterals.add(new MergeActionLiteral<>(EXISTS_SYMBOL, TruthValue.TRUE, List.of(parameter)));
		}
		for (var conjunct : conjuncts) {
			actionLiterals.add(new MergeActionLiteral<>(conjunct.symbol, conjunct.value, conjunct.arguments));
		}
		actionLiterals.addAll(originalActionLiterals);
		var refineAndPerformAction = new Action(parameters, List.copyOf(actionLiterals));

		var transitionRule = new Rule(rule.getName(), mayPrecondition, refineAndPerformAction);
		decisionRules.add(new DecisionRule(transitionRule));
		return this;
	}

	private record ConjunctValue(PartialSymbol<TruthValue, Boolean> symbol,
								 List<NodeVariable> arguments, TruthValue value) {
	}

	private List<ConjunctValue> singleClauseDnfToConjuncts(Dnf dnf, HashMap<NodeVariable, NodeVariable> argumentsMap) {
		var conjuncts = new ArrayList<ConjunctValue>();
		if (dnf.getClauses().size() != 1) {
			throw new IllegalArgumentException("Expected a single-clause DNF, got %d clauses instead"
					.formatted(dnf.getClauses().size()));
		}

		for (var literal : dnf.getClauses().getFirst().literals()) {
			switch (literal) {
			case CallLiteral callLiteral -> {
				var literalPolarity = callLiteral.getPolarity();
				var target = callLiteral.getTarget();
				var arguments = callLiteral.getArguments().stream().map(a -> argumentsMap.get((NodeVariable) a)).toList();

				switch (literalPolarity) {
				case TRANSITIVE ->
						throw new IllegalArgumentException("Unexpected transitive literal: %s".formatted(callLiteral));
				case POSITIVE -> {
					if (target instanceof Dnf targetDnf) {
						var innerConjuncts = singleClauseDnfToConjuncts(targetDnf, dnfParametersToArguments(targetDnf, arguments));
						conjuncts.addAll(innerConjuncts);
					} else if (target instanceof PartialRelation targetRelation) {
						conjuncts.add(new ConjunctValue(targetRelation, arguments, TruthValue.TRUE));
					} else {
						throw new IllegalArgumentException("Expected a partial relation or DNF, got %s instead"
								.formatted(callLiteral.getTarget()));
					}
				}
				case NEGATIVE -> {
					if (!(target instanceof PartialRelation targetRelation)) {
						throw new IllegalArgumentException("Expected a partial relation, got %s instead"
								.formatted(callLiteral.getTarget()));
					}
					conjuncts.add(new ConjunctValue(targetRelation, arguments, TruthValue.FALSE));
				}
				}
			}
			case ConstantLiteral ignored -> {
			}
			default -> throw new IllegalArgumentException("Unexpected literal %s".formatted(literal));
			}
		}
		return conjuncts;
	}

	private HashMap<NodeVariable, NodeVariable> dnfParametersToArguments(Dnf dnf, List<NodeVariable> arguments) {
		var argumentsMap = new HashMap<NodeVariable, NodeVariable>();
		var parameters = dnf.getSymbolicParameters();
		if (parameters.size() != arguments.size()) {
			throw new IllegalArgumentException("Expected %d arguments for DNF %s, got %d instead"
					.formatted(parameters.size(), dnf, arguments.size()));
		}
		for (int i = 0; i < parameters.size(); i++) {
			argumentsMap.put((NodeVariable) parameters.get(i).getVariable(), arguments.get(i));
		}
		return argumentsMap;
	}
}
