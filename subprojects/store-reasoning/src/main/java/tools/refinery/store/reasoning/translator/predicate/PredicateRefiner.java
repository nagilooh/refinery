/*
 * SPDX-FileCopyrightText: 2024 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.reasoning.translator.predicate;

import org.jetbrains.annotations.Nullable;
import tools.refinery.logic.dnf.RelationalQuery;
import tools.refinery.logic.term.truthvalue.TruthValue;
import tools.refinery.store.reasoning.ReasoningAdapter;
import tools.refinery.store.reasoning.refinement.ConcreteRelationRefiner;
import tools.refinery.store.reasoning.refinement.PartialInterpretationRefiner;
import tools.refinery.store.reasoning.refinement.RefinementUtils;
import tools.refinery.store.reasoning.representation.PartialRelation;
import tools.refinery.store.reasoning.representation.PartialSymbol;
import tools.refinery.store.reasoning.seed.ModelSeed;
import tools.refinery.store.reasoning.translator.RoundingMode;
import tools.refinery.store.representation.Symbol;
import tools.refinery.store.tuple.Tuple;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;

class PredicateRefiner extends ConcreteRelationRefiner {
	private final List<PartialRelation> parameterTypes;
	private final Set<PartialRelation> supertypes;
	private @Nullable PartialInterpretationRefiner<TruthValue, Boolean>[] parameterTypeRefiners;
	private PartialInterpretationRefiner<TruthValue, Boolean>[] supertypeRefiners;

	protected PredicateRefiner(
			ReasoningAdapter adapter, PartialSymbol<TruthValue, Boolean> partialSymbol,
			Symbol<TruthValue> concreteSymbol, List<PartialRelation> parameterTypes, Set<PartialRelation> supertypes,
			RoundingMode roundingMode) {
		super(adapter, partialSymbol, concreteSymbol, roundingMode);
		this.parameterTypes = parameterTypes;
		this.supertypes = supertypes;
	}

	protected PredicateRefiner(
			ReasoningAdapter adapter, PartialSymbol<TruthValue, Boolean> partialSymbol,
			Symbol<TruthValue> concreteSymbol, List<PartialRelation> parameterTypes, Set<PartialRelation> supertypes,
			RoundingMode roundingMode, List<AbstractionPropagation> abstractionPropagations,
			List<RefinementPropagation> refinementPropagations) {
		super(adapter, partialSymbol, concreteSymbol, roundingMode, abstractionPropagations, refinementPropagations);
		this.parameterTypes = parameterTypes;
		this.supertypes = supertypes;
	}

	@Override
	public void afterCreate() {
		super.afterCreate();
		int arity = parameterTypes.size();
		// Generic array creation.
		@SuppressWarnings("unchecked")
		PartialInterpretationRefiner<TruthValue, Boolean>[] array = new PartialInterpretationRefiner[arity];
		parameterTypeRefiners = array;
		var adapter = getAdapter();
		for (int i = 0; i < arity; i++) {
			var parameterType = parameterTypes.get(i);
			if (parameterType != null) {
				array[i] = adapter.getRefiner(parameterType);
			}
		}
		supertypeRefiners = RefinementUtils.getRefiners(adapter, supertypes);
	}

	@Override
	public boolean merge(Tuple key, TruthValue value) {
		var oldValue = get(key);
		var mergedValue = concretizationAwareMeet(oldValue, value);
		if (!Objects.equals(oldValue, mergedValue)) {
			put(key, mergedValue);
			if (!notifyRefinementListeners(key, mergedValue)) {
				return false;
			}
		}
		// Avoid cyclic propagation between parameter types by avoiding propagation after reaching a fixed point.
		if (mergedValue.must() && !oldValue.must()) {
			return refineParameters(key);
		}
		return true;
	}

	@Override
	public void afterInitialize(ModelSeed modelSeed) {
		var predicate = getPartialSymbol();
		var cursor = modelSeed.getCursor(predicate);
		while (cursor.move()) {
			var value = cursor.getValue();
			if (value.must()) {
				var key = cursor.getKey();
				if (!refineParameters(key)) {
					throw new IllegalArgumentException("Failed to merge type constraints of predicate %s for key %s"
							.formatted(predicate, key));
				}
			}
		}
	}

	private boolean refineParameters(Tuple key) {
		int arity = parameterTypeRefiners.length;
		for (int i = 0; i < arity; i++) {
			var refiner = parameterTypeRefiners[i];
			if (refiner != null && !refiner.merge(Tuple.of(key.get(i)), TruthValue.TRUE)) {
				return false;
			}
		}
		return RefinementUtils.mergeAll(supertypeRefiners, key, TruthValue.TRUE);
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
