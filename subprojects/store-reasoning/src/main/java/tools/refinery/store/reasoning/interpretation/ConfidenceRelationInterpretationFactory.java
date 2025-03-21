/*
 * SPDX-FileCopyrightText: 2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.reasoning.interpretation;

import tools.refinery.logic.term.truthvalue.TruthValue;
import tools.refinery.logic.term.truthvalue.TruthValueConfidence;
import tools.refinery.store.map.Cursor;
import tools.refinery.store.model.Interpretation;
import tools.refinery.store.reasoning.ReasoningAdapter;
import tools.refinery.store.reasoning.literal.Concreteness;
import tools.refinery.store.reasoning.representation.PartialSymbol;
import tools.refinery.store.reasoning.translator.RoundingMode;
import tools.refinery.store.representation.Symbol;
import tools.refinery.store.tuple.Tuple;

public class ConfidenceRelationInterpretationFactory implements PartialInterpretation.Factory<TruthValueConfidence,
		Boolean> {
	private final Symbol<TruthValueConfidence> symbol;
	private final RoundingMode roundingMode;

	public ConfidenceRelationInterpretationFactory(Symbol<TruthValueConfidence> symbol, RoundingMode roundingMode) {
		this.symbol = symbol;
		this.roundingMode = roundingMode;
	}

	@Override
	public PartialInterpretation<TruthValueConfidence, Boolean> create(
			ReasoningAdapter adapter, Concreteness concreteness, PartialSymbol<TruthValueConfidence, Boolean> partialSymbol) {
		var activeRoundingMode = concreteness == Concreteness.CANDIDATE ? roundingMode : RoundingMode.NONE;
		return new ConfidenceInterpretation(adapter, concreteness, partialSymbol, symbol, activeRoundingMode);
	}

	private static class ConfidenceInterpretation extends AbstractPartialInterpretation<TruthValueConfidence, Boolean> {
		private final Interpretation<TruthValueConfidence> interpretation;
		private final RoundingMode roundingMode;

		public ConfidenceInterpretation(ReasoningAdapter adapter, Concreteness concreteness,
										PartialSymbol<TruthValueConfidence, Boolean> partialSymbol,
										Symbol<TruthValueConfidence> symbol, RoundingMode roundingMode) {
			super(adapter, concreteness, partialSymbol);
			interpretation = adapter.getModel().getInterpretation(symbol);
			this.roundingMode = roundingMode;
		}

		private TruthValueConfidence getRounded(TruthValueConfidence confidence) {
			if (confidence.getTruthValue() != TruthValue.UNKNOWN) {
				return confidence;
			}
			return switch (roundingMode) {
				case NONE -> confidence;
				case PREFER_TRUE -> TruthValueConfidence.TRUE;
				case PREFER_FALSE -> TruthValueConfidence.FALSE;
			};
		}

		@Override
		public TruthValueConfidence get(Tuple key) {
			var confidenceValue = interpretation.get(key);
			return getRounded(confidenceValue);
		}

		@Override
		public Cursor<Tuple, TruthValueConfidence> getAll() {
			if (roundingMode == RoundingMode.NONE) {
				return interpretation.getAll();
			}
			return new Cursor<>() {
				private final Cursor<Tuple, TruthValueConfidence> cursor = interpretation.getAll();

				@Override
				public Tuple getKey() {
					return cursor.getKey();
				}

				@Override
				public TruthValueConfidence getValue() {
					return getRounded(cursor.getValue());
				}

				@Override
				public boolean isTerminated() {
					return cursor.isTerminated();
				}

				@Override
				public boolean move() {
					return cursor.move();
				}
			};
		}
	}
}
