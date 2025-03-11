/*
 * SPDX-FileCopyrightText: 2023-2024 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.reasoning.refinement;

import tools.refinery.logic.term.truthvalue.TruthValue;
import tools.refinery.logic.term.truthvalue.TruthValueConfidence;
import tools.refinery.store.model.Interpretation;
import tools.refinery.store.reasoning.ReasoningAdapter;
import tools.refinery.store.reasoning.representation.PartialSymbol;
import tools.refinery.store.reasoning.translator.RoundingMode;
import tools.refinery.store.representation.Symbol;
import tools.refinery.store.tuple.Tuple;

import java.util.Objects;

public class ConcreteRelationConfidenceRefiner extends
		AbstractPartialInterpretationRefiner.ConcretizationAware<TruthValueConfidence, Boolean> {
	private final Interpretation<TruthValueConfidence> interpretation;
	private final RoundingMode roundingMode;

	protected ConcreteRelationConfidenceRefiner(ReasoningAdapter adapter, PartialSymbol<TruthValueConfidence, Boolean> partialSymbol,
                                                Symbol<TruthValueConfidence> concreteSymbol, RoundingMode roundingMode) {
		super(adapter, partialSymbol);
		interpretation = adapter.getModel().getInterpretation(concreteSymbol);
		this.roundingMode = roundingMode;
	}

	@Override
	public boolean merge(Tuple key, TruthValueConfidence value) {
		var currentValue = get(key);
		var mergedValue = concretizationAwareMeet(currentValue, value);
		if (!Objects.equals(currentValue, mergedValue)) {
			put(key, mergedValue);
		}
		return true;
	}

	protected TruthValueConfidence concretizationAwareMeet(TruthValueConfidence currentValue, TruthValueConfidence value) {
		return forbiddenByConcretization(currentValue, value) ? new TruthValueConfidence(TruthValue.ERROR, 1.0) :
				currentValue.meet(value);
	}

	protected boolean forbiddenByConcretization(TruthValueConfidence oldValue, TruthValueConfidence newValue) {
		return shouldCheckConcretization(oldValue, newValue) && concretizationInProgress();
	}

	protected boolean shouldCheckConcretization(TruthValueConfidence oldValue, TruthValueConfidence newValue) {
		return switch (roundingMode) {
			case NONE -> false;
			case PREFER_FALSE -> !oldValue.must() && newValue.getTruthValue() == TruthValue.TRUE;
			case PREFER_TRUE -> oldValue.may() && newValue.getTruthValue() == TruthValue.FALSE;
		};
	}

	protected TruthValueConfidence get(Tuple key) {
		return interpretation.get(key);
	}

	protected TruthValueConfidence put(Tuple key, TruthValueConfidence value) {
		return interpretation.put(key, value);
	}

	public static Factory<TruthValueConfidence, Boolean> of(Symbol<TruthValueConfidence> concreteSymbol, RoundingMode roundingMode) {
		if (roundingMode == RoundingMode.NONE) {
			return ConcreteSymbolRefiner.of(concreteSymbol);
		}
		return (adapter, partialSymbol) -> new ConcreteRelationConfidenceRefiner(adapter, partialSymbol, concreteSymbol,
				roundingMode);
	}
}
