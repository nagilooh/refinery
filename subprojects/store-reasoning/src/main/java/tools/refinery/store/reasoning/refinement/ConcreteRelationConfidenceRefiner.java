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
		AbstractPartialInterpretationRefiner.ConcretizationAware<TruthValue, Boolean> {
	private final Interpretation<TruthValueConfidence> interpretation;
	private final RoundingMode roundingMode;
	private static Double confidenceCost = 0.0;

	protected ConcreteRelationConfidenceRefiner(ReasoningAdapter adapter, PartialSymbol<TruthValue, Boolean> partialSymbol,
                                                Symbol<TruthValueConfidence> concreteSymbol, RoundingMode roundingMode) {
		super(adapter, partialSymbol);
		interpretation = adapter.getModel().getInterpretation(concreteSymbol);
		this.roundingMode = roundingMode;
	}

	private static void increaseConfidenceCost(double cost) {
		if (!confidenceCost.isNaN()) {
			confidenceCost += cost;
		}
	}

	@Override
	public boolean merge(Tuple key, TruthValue value) {
		var currentValue = get(key);
		var mergedValue = concretizationAwareMeet(currentValue, value);
		if (!Objects.equals(currentValue, mergedValue)) {
			put(key, mergedValue);
			increaseConfidenceCost(Math.log(Math.abs(mergedValue.getConfidence() - currentValue.getConfidence())));
		}
		return true;
	}

	protected TruthValueConfidence concretizationAwareMeet(TruthValueConfidence currentValue, TruthValue value) {
		if (forbiddenByConcretization(currentValue, value)) {
			return TruthValueConfidence.ERROR;
		}
		return switch (value) {
			case UNKNOWN -> currentValue;
			case ERROR -> TruthValueConfidence.ERROR;
			case TRUE -> currentValue.may() ? TruthValueConfidence.TRUE : TruthValueConfidence.ERROR;
			case FALSE -> currentValue.must() ? TruthValueConfidence.ERROR : TruthValueConfidence.FALSE;
		};
	}

	protected boolean forbiddenByConcretization(TruthValueConfidence oldValue, TruthValue newValue) {
		return shouldCheckConcretization(oldValue, newValue) && concretizationInProgress();
	}

	protected boolean shouldCheckConcretization(TruthValueConfidence oldValue, TruthValue newValue) {
		return switch (roundingMode) {
			case NONE -> false;
			case PREFER_FALSE -> !oldValue.must() && newValue == TruthValue.TRUE;
			case PREFER_TRUE -> oldValue.may() && newValue == TruthValue.FALSE;
		};
	}

	protected TruthValueConfidence get(Tuple key) {
		return interpretation.get(key);
	}

	protected TruthValueConfidence put(Tuple key, TruthValueConfidence value) {
		return interpretation.put(key, value);
	}

	public static Factory<TruthValue, Boolean> of(Symbol<TruthValueConfidence> concreteSymbol, RoundingMode roundingMode) {
		return (adapter, partialSymbol) -> new ConcreteRelationConfidenceRefiner(adapter, partialSymbol, concreteSymbol,
				roundingMode);
	}

	public static Double getConfidenceCost() {
		return confidenceCost;
	}
}
