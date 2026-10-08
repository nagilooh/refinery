/*
 * SPDX-FileCopyrightText: 2024 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.reasoning.translator.predicate;

import tools.refinery.logic.dnf.RelationalQuery;
import tools.refinery.logic.term.truthvalue.TruthValue;
import tools.refinery.store.reasoning.ReasoningAdapter;
import tools.refinery.store.reasoning.refinement.ConcreteRelationRefiner;
import tools.refinery.store.reasoning.representation.PartialRelation;
import tools.refinery.store.reasoning.representation.PartialSymbol;
import tools.refinery.store.reasoning.translator.RoundingMode;
import tools.refinery.store.representation.Symbol;
import tools.refinery.store.tuple.Tuple;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static tools.refinery.logic.term.truthvalue.TruthValue.TRUE;

class PredicateRefiner extends ConcreteRelationRefiner {

	protected PredicateRefiner(
			ReasoningAdapter adapter, PartialSymbol<TruthValue, Boolean> partialSymbol,
			Symbol<TruthValue> concreteSymbol, List<PartialRelation> parameterTypes, Set<PartialRelation> supertypes,
			RoundingMode roundingMode) {
		this(adapter, partialSymbol, concreteSymbol, parameterTypes, supertypes, roundingMode, List.of(), List.of());
	}

	protected PredicateRefiner(
			ReasoningAdapter adapter, PartialSymbol<TruthValue, Boolean> partialSymbol,
			Symbol<TruthValue> concreteSymbol, List<PartialRelation> parameterTypes, Set<PartialRelation> supertypes,
			RoundingMode roundingMode, List<AbstractionPropagation> abstractionPropagations,
			List<RefinementPropagation> refinementPropagations) {
		var typePropagations = getTypePropagations(parameterTypes, supertypes);
		List<RefinementPropagation> combinedRefinementPropagations =
				new ArrayList<>(refinementPropagations.size() + typePropagations.size());
		combinedRefinementPropagations.addAll(refinementPropagations);
		combinedRefinementPropagations.addAll(typePropagations);
		super(adapter, partialSymbol, concreteSymbol, roundingMode, abstractionPropagations, combinedRefinementPropagations);
	}

	private static List<RefinementPropagation> getTypePropagations(List<PartialRelation> parameterTypes,
																   Set<PartialRelation> supertypes) {
		// Avoid cyclic propagation between parameter types by avoiding propagation after reaching a fixed point.
		var result = new ArrayList<RefinementPropagation>(parameterTypes.size() + supertypes.size());
		int arity = parameterTypes.size();
		for (int i = 0; i < arity; i++) {
			var parameterType = parameterTypes.get(i);
			if (parameterType != null) {
				result.add(new RefinementPropagation(parameterType, TruthValue::must, new int[]{i},
						new int[]{-1}, TRUE));
			}
		}
		for (var superType : supertypes) {
			result.add(new RefinementPropagation(superType, TruthValue::must, Tuple.identityProjection(arity),
					new int[]{-1}, TRUE));
		}
		return result;
	}

	public static Factory<TruthValue, Boolean> of(
			Symbol<TruthValue> concreteSymbol, List<PartialRelation> parameterTypes, Set<PartialRelation> supertypes,
			RoundingMode roundingMode) {
		return (adapter, partialSymbol) -> new PredicateRefiner(adapter, partialSymbol, concreteSymbol,
				parameterTypes, supertypes, roundingMode);
	}

	public static Factory<TruthValue, Boolean> of(
			Symbol<TruthValue> concreteSymbol, List<PartialRelation> parameterTypes, Set<PartialRelation> supertypes,
			RoundingMode roundingMode, RelationalQuery query) {
		return (adapter, partialSymbol) -> {
			var abstractionPropagations = new ArrayList<AbstractionPropagation>();
			var refinementPropagations = new ArrayList<RefinementPropagation>();
			collectPropagations(partialSymbol, query, abstractionPropagations, refinementPropagations);
			return new PredicateRefiner(adapter, partialSymbol, concreteSymbol,
					parameterTypes, supertypes, roundingMode, abstractionPropagations, refinementPropagations);
		};
	}
}
