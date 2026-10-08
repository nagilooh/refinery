/*
 * SPDX-FileCopyrightText: 2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.reasoning.translator.crossreference;

import tools.refinery.logic.term.truthvalue.TruthValue;
import tools.refinery.store.reasoning.ReasoningAdapter;
import tools.refinery.store.reasoning.refinement.ConcreteRelationRefiner;
import tools.refinery.store.reasoning.refinement.TypeConstraintRefiner;
import tools.refinery.store.reasoning.representation.PartialRelation;
import tools.refinery.store.reasoning.representation.PartialSymbol;
import tools.refinery.store.reasoning.seed.ModelSeed;
import tools.refinery.store.reasoning.translator.RoundingMode;
import tools.refinery.store.representation.Symbol;
import tools.refinery.store.tuple.Tuple;

import java.util.Objects;
import java.util.Set;

class UndirectedCrossReferenceRefiner extends ConcreteRelationRefiner {
	private final PartialRelation sourceType;
	private final Set<PartialRelation> supersets;
	private TypeConstraintRefiner typeConstraintRefiner;

	protected UndirectedCrossReferenceRefiner(
			ReasoningAdapter adapter, PartialSymbol<TruthValue, Boolean> partialSymbol,
			Symbol<TruthValue> concreteSymbol, UndirectedCrossReferenceInfo info, RoundingMode roundingMode) {
		super(adapter, partialSymbol, concreteSymbol, roundingMode);
		this.sourceType = info.type();
		this.supersets = info.supersets();
	}

	@Override
	public void afterCreate() {
		super.afterCreate();
		var adapter = getAdapter();
		typeConstraintRefiner = new TypeConstraintRefiner(adapter, sourceType, sourceType, supersets, supersets);
	}

	@Override
	public boolean merge(Tuple key, TruthValue value) {
		int source = key.get(0);
		int target = key.get(1);
		var oldValue = get(key);
		var mergedValue = concretizationAwareMeet(oldValue, value);
		if (!Objects.equals(oldValue, mergedValue)) {
			put(key, mergedValue);
			if (source != target) {
				var oppositeKey = Tuple.of(target, source);
				var inverseOldValue = put(oppositeKey, mergedValue);
				if (!Objects.equals(oldValue, inverseOldValue)) {
					return false;
				}

				if (!(notifyRefinementListeners(key, mergedValue, oldValue) &&
						notifyRefinementListeners(oppositeKey, mergedValue, oldValue))) {
					return false;
				}
			} else {
				if (!notifyRefinementListeners(key, mergedValue, oldValue)) {
					return false;
				}
			}
		}
		if (value.must()) {
			return typeConstraintRefiner.merge(key);
		}
		return true;
	}

	@Override
	public boolean join(Tuple key, TruthValue value) {
		if (concretizationInProgress()) {
			return false;
		}

		int source = key.get(0);
		int target = key.get(1);
		var oldValue = get(key);
		var joinedValue = oldValue.join(value);
		if (!Objects.equals(oldValue, joinedValue)) {
			put(key, joinedValue);
			if (source != target) {
				var oppositeKey = Tuple.of(target, source);
				var inverseOldValue = put(oppositeKey, joinedValue);
				if (!Objects.equals(oldValue, inverseOldValue)) {
					return false;
				}

				return notifyAbstractionListeners(key, oldValue, joinedValue) &&
						notifyAbstractionListeners(oppositeKey, inverseOldValue, joinedValue);
			} else {
				return notifyAbstractionListeners(key, oldValue, joinedValue);
			}
		}
		return true;
	}

	@Override
	public void afterInitialize(ModelSeed modelSeed) {
		var linkType = getPartialSymbol();
		typeConstraintRefiner.mergeFromSeed(linkType, modelSeed);
	}

	public static Factory<TruthValue, Boolean> of(Symbol<TruthValue> concreteSymbol, UndirectedCrossReferenceInfo info,
	                                              RoundingMode roundingMode) {
		return (adapter, partialSymbol) -> new UndirectedCrossReferenceRefiner(adapter, partialSymbol, concreteSymbol,
				info, roundingMode);
	}
}
