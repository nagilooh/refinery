/*
 * SPDX-FileCopyrightText: 2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.reasoning.interpretation;

import tools.refinery.logic.term.truthvalue.TruthValueConfidence;
import tools.refinery.store.map.Cursor;
import tools.refinery.store.reasoning.ReasoningAdapter;
import tools.refinery.store.reasoning.literal.Concreteness;
import tools.refinery.store.reasoning.representation.PartialSymbol;
import tools.refinery.store.tuple.Tuple;

public class QueryBasedConfidenceRelationInterpretationFactory implements PartialInterpretation.Factory<TruthValueConfidence,
		Boolean> {

	@Override
	public PartialInterpretation<TruthValueConfidence, Boolean> create(
			ReasoningAdapter adapter, Concreteness concreteness, PartialSymbol<TruthValueConfidence, Boolean> partialSymbol) {
		return new ConfidenceInterpretation(adapter, concreteness, partialSymbol);
	}

	private static class ConfidenceInterpretation extends AbstractPartialInterpretation<TruthValueConfidence, Boolean> {
		public ConfidenceInterpretation(ReasoningAdapter adapter, Concreteness concreteness,
										PartialSymbol<TruthValueConfidence, Boolean> partialSymbol) {
			super(adapter, concreteness, partialSymbol);
		}

		@Override
		public TruthValueConfidence get(Tuple key) {
			return fail();
		}

		@Override
		public Cursor<Tuple, TruthValueConfidence> getAll() {
			return fail();
		}

		private <T> T fail() {
			throw new UnsupportedOperationException("No interpretation for partial symbol: " + getPartialSymbol());
		}
	}
}
