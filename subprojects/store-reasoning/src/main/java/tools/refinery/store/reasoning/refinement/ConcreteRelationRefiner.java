/*
 * SPDX-FileCopyrightText: 2023-2026 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.reasoning.refinement;

import tools.refinery.logic.Constraint;
import tools.refinery.logic.dnf.Dnf;
import tools.refinery.logic.dnf.RelationalQuery;
import tools.refinery.logic.literal.AbstractCallLiteral;
import tools.refinery.logic.literal.CallLiteral;
import tools.refinery.logic.literal.CallPolarity;
import tools.refinery.logic.literal.Literal;
import tools.refinery.logic.literal.TermLiteral;
import tools.refinery.logic.term.AbstractCallTerm;
import tools.refinery.logic.term.BinaryTerm;
import tools.refinery.logic.term.ConstantTerm;
import tools.refinery.logic.term.NodeIdTerm;
import tools.refinery.logic.term.Term;
import tools.refinery.logic.term.UnaryTerm;
import tools.refinery.logic.term.Variable;
import tools.refinery.logic.term.truthvalue.TruthValue;
import tools.refinery.store.model.Interpretation;
import tools.refinery.store.query.view.AnySymbolView;
import tools.refinery.store.reasoning.ReasoningAdapter;
import tools.refinery.store.reasoning.literal.ModalConstraint;
import tools.refinery.store.reasoning.literal.PartialFunctionCallTerm;
import tools.refinery.store.reasoning.representation.PartialRelation;
import tools.refinery.store.reasoning.representation.PartialSymbol;
import tools.refinery.store.reasoning.translator.RoundingMode;
import tools.refinery.store.representation.Symbol;
import tools.refinery.store.tuple.Tuple;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;

import static tools.refinery.logic.term.truthvalue.TruthValue.FALSE;
import static tools.refinery.logic.term.truthvalue.TruthValue.TRUE;
import static tools.refinery.logic.term.truthvalue.TruthValue.UNKNOWN;

public class ConcreteRelationRefiner extends
		AbstractPartialInterpretationRefiner.ConcretizationAware<TruthValue, Boolean> {
	private final Interpretation<TruthValue> interpretation;
	private final RoundingMode roundingMode;
	private final AbstractionPropagation[] abstractionPropagations;
	private final RefinementPropagation[] refinementPropagations;

	protected ConcreteRelationRefiner(ReasoningAdapter adapter, PartialSymbol<TruthValue, Boolean> partialSymbol,
	                                  Symbol<TruthValue> concreteSymbol, RoundingMode roundingMode) {
		super(adapter, partialSymbol);
		interpretation = adapter.getModel().getInterpretation(concreteSymbol);
		this.roundingMode = roundingMode;
		this.abstractionPropagations = new AbstractionPropagation[0];
		this.refinementPropagations = new RefinementPropagation[0];
	}

	protected ConcreteRelationRefiner(ReasoningAdapter adapter, PartialSymbol<TruthValue, Boolean> partialSymbol,
	                                  Symbol<TruthValue> concreteSymbol, RoundingMode roundingMode,
	                                  List<AbstractionPropagation> abstractionPropagations,
	                                  List<RefinementPropagation> refinementPropagations) {
		super(adapter, partialSymbol);
		interpretation = adapter.getModel().getInterpretation(concreteSymbol);
		this.roundingMode = roundingMode;
		this.abstractionPropagations = new AbstractionPropagation[abstractionPropagations.size()];
		for (int i = 0; i < abstractionPropagations.size(); i++) {
			this.abstractionPropagations[i] = abstractionPropagations.get(i);
		}
		this.refinementPropagations = new RefinementPropagation[refinementPropagations.size()];
		for (int i = 0; i < refinementPropagations.size(); i++) {
			this.refinementPropagations[i] = refinementPropagations.get(i);
		}
	}

	@Override
	public boolean merge(Tuple key, TruthValue value) {
		var oldValue = get(key);
		var mergedValue = concretizationAwareMeet(oldValue, value);
		if (!Objects.equals(oldValue, mergedValue)) {
			put(key, mergedValue);
			return notifyRefinementListeners(key, mergedValue);
		}
		return true;
	}

	@Override
	public boolean join(Tuple key, TruthValue value) {
		if (concretizationInProgress()) {
			return false;
		}

		var oldValue = get(key);
		var joinedValue = oldValue.join(value);
		if (!Objects.equals(oldValue, joinedValue)) {
			put(key, joinedValue);
		}
		return notifyAbstractionListeners(key, oldValue, joinedValue);
	}

	@Override
	public void afterCreate() {
		for (var abstractionPropagation : abstractionPropagations) {
			var refiner = getAdapter().getRefiner(abstractionPropagation.partialSymbol());
			var argumentMapping = abstractionPropagation.argumentMapping();
			refiner.addAbstractionListener((key, _, _) -> {
				int[] fixedArguments = new int[argumentMapping.length];
				for (int i = 0; i < argumentMapping.length; ++i) {
					if (argumentMapping[i] == -1) {
						fixedArguments[i] = -1;
					} else {
						fixedArguments[i] = key.get(argumentMapping[i]);
					}
				}
				return joinAll(fixedArguments, tuple -> join(tuple, UNKNOWN));
			});
		}
	}

	protected TruthValue concretizationAwareMeet(TruthValue currentValue, TruthValue value) {
		return forbiddenByConcretization(currentValue, value) ? TruthValue.ERROR : currentValue.meet(value);
	}

	protected boolean forbiddenByConcretization(TruthValue oldValue, TruthValue newValue) {
		return shouldCheckConcretization(oldValue, newValue) && concretizationInProgress();
	}

	protected boolean shouldCheckConcretization(TruthValue oldValue, TruthValue newValue) {
		return switch (roundingMode) {
			case NONE -> false;
			case PREFER_FALSE -> !oldValue.must() && newValue == TRUE;
			case PREFER_TRUE -> oldValue.may() && newValue == FALSE;
		};
	}

	protected TruthValue get(Tuple key) {
		return interpretation.get(key);
	}

	protected TruthValue put(Tuple key, TruthValue value) {
		return interpretation.put(key, value);
	}

	protected boolean notifyRefinementListeners(Tuple key, TruthValue mergedValue) {
		for (var refinementPropagation : refinementPropagations) {
			if (mergedValue == refinementPropagation.refineOnValue) {
				var refiner = getAdapter().getRefiner(refinementPropagation.relation);
				var projectedKey = key.map(refinementPropagation.projection);
				if (!refiner.merge(projectedKey, refinementPropagation.mergedValue)) {
					return false;
				}
			}
		}
		return true;
	}

	public AbstractionPropagation[] getAbstractionPropagations() {
		return abstractionPropagations;
	}

	public static Factory<TruthValue, Boolean> of(Symbol<TruthValue> concreteSymbol,
	                                              RelationalQuery query, RoundingMode roundingMode) {
		if (roundingMode == RoundingMode.NONE) {
			return ConcreteSymbolRefiner.of(concreteSymbol);
		}

		return (adapter, partialSymbol) -> {
			var abstractionPropagations = new ArrayList<AbstractionPropagation>();
			var refinementPropagations = new ArrayList<RefinementPropagation>();
			collectPropagations(partialSymbol, query, abstractionPropagations, refinementPropagations);
			return new ConcreteRelationRefiner(adapter, partialSymbol, concreteSymbol, roundingMode,
					abstractionPropagations, refinementPropagations);
		};
	}

	protected static void collectPropagations(PartialSymbol<TruthValue, Boolean> partialSymbol, RelationalQuery query,
	                                          List<AbstractionPropagation> abstractionPropagations,
	                                          List<RefinementPropagation> refinementPropagations) {
		if (query == null) {
			return;
		}

		var dnf = query.getDnf();
		var projection = new LinkedHashMap<Variable, Integer>();
		var parameters = dnf.getSymbolicParameters();
		for (int i = 0; i < dnf.arity(); ++i) {
			projection.put(parameters.get(i).getVariable(), i);
		}
		collectPropagations(partialSymbol.arity(), query.getDnf(), projection, true,
				abstractionPropagations, refinementPropagations);
	}

	/**
	 * Register refiner listeners for derived relations considering all projections to other relations in its query
	 * with the relevant polarity.
	 * <ul>
	 * <li>
	 *     If the symbol of this refiner is refined to false then all clauses should be false. Practically, if a
	 *     clause consists of a single literal, the literal should also be refined to false.
	 * </li>
	 * <li>
	 *     If the symbol of this refiner is refined to true and the symbol is derived by a query with a single
	 *     clause, then all literals of that clause should be refined to true.
	 * </li>
	 * <li>
	 *     If this symbol depends on another symbol and the other symbol is abstracted, then this symbol also
	 *     needs to be abstracted according to the projection and the polarity.
	 * </li>
	 * </ul>
	 * Downwards refinement propagation is "best-effort": propagation rules may be used to implement more
	 * sophisticated reasoning. Upwards abstraction propagation is safe and potentially over-approximating.
	 *
	 * @param dnf                  current DNF
	 * @param projection           the projection of parameters of the original parameters (indices) to the current
	 *                             variables
	 * @param isProjectionPositive the polarity of the projection
	 */
	protected static void collectPropagations(int arity, Dnf dnf,
	                                          Map<Variable, Integer> projection,
	                                          Boolean isProjectionPositive,
	                                          List<AbstractionPropagation> abstractionPropagations,
	                                          List<RefinementPropagation> refinementPropagations) {
		for (var clause : dnf.getClauses()) {
			for (var literal : clause.literals()) {
				switch (literal) {
				case AbstractCallLiteral abstractCallLiteral -> {
					var arguments = abstractCallLiteral.getArguments();
					var polarity = abstractCallLiteral instanceof CallLiteral callLiteral ?
							callLiteral.getPolarity() : CallPolarity.POSITIVE;

					switch (abstractCallLiteral.getTarget()) {
					case PartialRelation relation -> {
						// Join derived value if base value is abstracted
						var argumentMapping = getArgumentMapping(arity, projection, arguments);
						abstractionPropagations.add(new AbstractionPropagation(relation, argumentMapping));

						if (isProjectionPositive != null) {
							// Merge literal true if the dnf consists of a single clause when the dnf is refined to true
							if (dnf.getClauses().size() == 1) {
								var refineWhenToValue = isProjectionPositive ? TRUE : FALSE;
								var mergedValue = polarity.isPositive() ? TRUE : FALSE;
								refineIfPossible(refinementPropagations, relation, projection, arguments, refineWhenToValue, mergedValue);
							}

							// Merge clause false if it consists of a single literal when the dnf is refined to false
							if (clause.literals().size() == 1) {
								var refineWhenToValue = isProjectionPositive ? FALSE : TRUE;
								var mergedValue = polarity.isPositive() ? FALSE : TRUE;
								refineIfPossible(refinementPropagations, relation, projection, arguments, refineWhenToValue, mergedValue);
							}
						}
					}
					case Dnf targetDnf -> {
						var updatedProjection = updateProjection(projection, arguments, targetDnf);
						Boolean isPositive = isProjectionPositive == null ? null : switch (polarity) {
							case POSITIVE -> isProjectionPositive;
							case NEGATIVE -> !isProjectionPositive;
							case TRANSITIVE -> null;
						};
						collectPropagations(arity, targetDnf, updatedProjection, isPositive,
								abstractionPropagations, refinementPropagations);
					}
					case AnySymbolView ignored -> {
					}
					default -> throw new UnsupportedOperationException("Unsupported call literal target.");
					}
				}
				case TermLiteral<?> termLiteral ->
						collectPropagations(termLiteral.getTerm(), arity, projection, abstractionPropagations);
				default -> {
				}
				}
			}
		}
	}

	private static void collectPropagations(Term<?> term, int arity, Map<Variable, Integer> projection,
	                                        List<AbstractionPropagation> abstractionPropagations) {
		switch (term) {
		case PartialFunctionCallTerm<?, ?> partialFunctionCallTerm -> {
			var arguments = partialFunctionCallTerm.getArguments();
			var argumentMapping = getArgumentMapping(arity, projection, arguments);
			var abstractionPropagation = new AbstractionPropagation(partialFunctionCallTerm.getPartialFunction(), argumentMapping);
			abstractionPropagations.add(abstractionPropagation);
		}
		case UnaryTerm<?, ?> unaryTerm ->
				collectPropagations(unaryTerm.getBody(), arity, projection, abstractionPropagations);
		case BinaryTerm<?, ?, ?> binaryTerm -> {
			collectPropagations(binaryTerm.getLeft(), arity, projection, abstractionPropagations);
			collectPropagations(binaryTerm.getRight(), arity, projection, abstractionPropagations);
		}
		case AbstractCallTerm<?> abstractCallTerm -> {
			var target = abstractCallTerm.getTarget();
			var arguments = abstractCallTerm.getArguments();
			collectPropagations(target, arity, projection, arguments, abstractionPropagations);
		}
		case ConstantTerm<?> ignored -> {
		}
		case NodeIdTerm ignored -> {
		}
		default -> throw new UnsupportedOperationException("Unsupported term.");
		}
	}

	private static void collectPropagations(Constraint constraint, int arity, Map<Variable, Integer> projection,
	                                        List<Variable> arguments,
	                                        List<AbstractionPropagation> abstractionPropagations) {
		switch (constraint) {
		case PartialRelation relation -> {
			var argumentMapping = getArgumentMapping(arity, projection, arguments);
			abstractionPropagations.add(new AbstractionPropagation(relation, argumentMapping));
		}
		case Dnf dnf -> {
			var updatedProjection = updateProjection(projection, arguments, dnf);
			for (var clause : dnf.getClauses()) {
				for (var literal : clause.literals()) {
					collectPropagations(literal, arity, updatedProjection, abstractionPropagations);
				}
			}
		}
		case ModalConstraint modalConstraint ->
				collectPropagations(modalConstraint.constraint(), arity, projection, arguments, abstractionPropagations);
		case AnySymbolView ignored -> {
		}
		default -> throw new UnsupportedOperationException("Unsupported constraint.");
		}
	}

	private static void collectPropagations(Literal literal, int arity, Map<Variable, Integer> projection,
	                                        List<AbstractionPropagation> abstractionPropagations) {
		switch (literal) {
		case AbstractCallLiteral abstractCallLiteral ->
				collectPropagations(abstractCallLiteral.getTarget(), arity, projection,
						abstractCallLiteral.getArguments(), abstractionPropagations);
		case TermLiteral<?> termLiteral ->
				collectPropagations(termLiteral.getTerm(), arity, projection, abstractionPropagations);
		default -> {
		}
		}
	}

	private static int[] getArgumentMapping(int arity, Map<Variable, Integer> projection,
	                                        List<? extends Variable> arguments) {
		int[] argumentMapping = new int[arity];
		Arrays.fill(argumentMapping, -1);
		for (var entry : projection.entrySet()) {
			var argumentIndex = arguments.indexOf(entry.getKey());
			if (argumentIndex != -1) {
				argumentMapping[entry.getValue()] = argumentIndex;
			}
		}
		return argumentMapping;
	}

	private static Map<Variable, Integer> updateProjection(Map<Variable, Integer> projection,
	                                                       List<Variable> arguments, Dnf dnf) {
		var updatedProjection = new LinkedHashMap<Variable, Integer>();
		var parameters = dnf.getSymbolicParameters();
		for (int i = 0; i < dnf.arity(); ++i) {
			var originalIndex = projection.get(arguments.get(i));
			if (originalIndex != null) {
				updatedProjection.put(parameters.get(i).getVariable(), originalIndex);
			}
		}
		return updatedProjection;
	}

	private boolean joinAll(int[] argumentMapping, Function<Tuple, Boolean> joinAction) {
		int arity = getPartialSymbol().arity();
		int nodeCount = getAdapter().getNodeCount();

		if (getPartialSymbol().defaultValue() == UNKNOWN) {
			// we may use the cursor of the interpretation for the abstraction
			int permutationIterationCount = 1;
			int minAdjacentSize = -1;
			int minAdjacentIndex = -1;
			for (int i = 0; i < arity; i++) {
				if (argumentMapping[i] == -1) {
					permutationIterationCount *= nodeCount;
				} else {
					int adjacentSize = interpretation.getAdjacentSize(i, argumentMapping[i]);
					if (minAdjacentSize == -1 || adjacentSize < minAdjacentSize) {
						minAdjacentSize = adjacentSize;
						minAdjacentIndex = i;
					}
				}
			}

			if (minAdjacentSize != -1 && minAdjacentSize < permutationIterationCount) {
				// better to iterate over the interpretation elements
				var cursor = interpretation.getAdjacent(minAdjacentIndex, argumentMapping[minAdjacentIndex]);
				while (cursor.move()) {
					var key = cursor.getKey();
					var joinKey = true;
					for (int i = 0; i < arity; i++) {
						if (argumentMapping[i] != -1 && argumentMapping[i] != key.get(i)) {
							joinKey = false;
						}
					}
					if (joinKey) {
						if (!joinAction.apply(key)) {
							return false;
						}
					}
				}
				return true;
			}
		}

		return backtrack(nodeCount, arity, 0, argumentMapping, new int[arity], joinAction);
	}

	private static boolean backtrack(int n, int k, int depth,
	                                 int[] fixedArr,
	                                 int[] current,
	                                 Function<Tuple, Boolean> consumer) {

		if (depth == k) {
			return consumer.apply(Tuple.of(current));
		}

		int fixedVal = fixedArr[depth];
		if (fixedVal != -1) {
			current[depth] = fixedVal;
			return backtrack(n, k, depth + 1, fixedArr, current, consumer);
		}

		for (int i = 0; i < n; i++) {
			current[depth] = i;
			if (!backtrack(n, k, depth + 1, fixedArr, current, consumer)) {
				return false;
			}
		}
		return true;
	}

	private static void refineIfPossible(List<RefinementPropagation> refinementPropagations,
	                                     PartialRelation relation, Map<Variable, Integer> projection,
	                                     List<Variable> arguments, TruthValue refineOnValue,
	                                     TruthValue mergedValue) {
		boolean anyExistentiallyQuantified = false;
		int[] callProjection = new int[relation.arity()];
		for (int i = 0; i < relation.arity(); ++i) {
			Integer originalIndex = projection.get(arguments.get(i));
			if (originalIndex == null) {
				anyExistentiallyQuantified = true;
				break;
			}
			callProjection[i] = originalIndex;
		}

		if (!anyExistentiallyQuantified) {
			refinementPropagations.add(new RefinementPropagation(relation, refineOnValue, callProjection, mergedValue));
		}
	}

	public record AbstractionPropagation(PartialSymbol<?, ?> partialSymbol, int[] argumentMapping) {
	}

	protected record RefinementPropagation(PartialRelation relation, TruthValue refineOnValue, int[] projection,
	                                       TruthValue mergedValue) {
	}
}
