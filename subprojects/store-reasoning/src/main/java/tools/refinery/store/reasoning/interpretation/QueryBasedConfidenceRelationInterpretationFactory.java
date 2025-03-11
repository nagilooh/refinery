/*
 * SPDX-FileCopyrightText: 2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.reasoning.interpretation;

import tools.refinery.logic.dnf.Query;
import tools.refinery.logic.term.truthvalue.TruthValue;
import tools.refinery.logic.term.truthvalue.TruthValueConfidence;
import tools.refinery.store.map.Cursor;
import tools.refinery.store.model.ModelStoreBuilder;
import tools.refinery.store.query.ModelQueryAdapter;
import tools.refinery.store.query.ModelQueryBuilder;
import tools.refinery.store.query.resultset.ResultSet;
import tools.refinery.store.reasoning.ReasoningAdapter;
import tools.refinery.store.reasoning.literal.Concreteness;
import tools.refinery.store.reasoning.representation.PartialSymbol;
import tools.refinery.store.tuple.Tuple;

import java.util.Set;

public class QueryBasedConfidenceRelationInterpretationFactory implements PartialInterpretation.Factory<TruthValueConfidence,
		Boolean> {
	private final Query<Boolean> may;
	private final Query<Boolean> must;
	private final Query<Boolean> candidateMay;
	private final Query<Boolean> candidateMust;

	public QueryBasedConfidenceRelationInterpretationFactory(
			Query<Boolean> may, Query<Boolean> must, Query<Boolean> candidateMay, Query<Boolean> candidateMust) {
		this.may = may;
		this.must = must;
		this.candidateMay = candidateMay;
		this.candidateMust = candidateMust;
	}

	@Override
	public PartialInterpretation<TruthValueConfidence, Boolean> create(
			ReasoningAdapter adapter, Concreteness concreteness, PartialSymbol<TruthValueConfidence, Boolean> partialSymbol) {
		var queryEngine = adapter.getModel().getAdapter(ModelQueryAdapter.class);
		ResultSet<Boolean> mayResultSet;
		ResultSet<Boolean> mustResultSet;
		switch (concreteness) {
		case PARTIAL -> {
			mayResultSet = queryEngine.getResultSet(may);
			mustResultSet = queryEngine.getResultSet(must);
		}
		case CANDIDATE -> {
			mayResultSet = queryEngine.getResultSet(candidateMay);
			mustResultSet = queryEngine.getResultSet(candidateMust);
		}
		default -> throw new IllegalArgumentException("Unknown concreteness: " + concreteness);
		}
		if (mayResultSet.equals(mustResultSet)) {
			return new TwoValuedInterpretation(adapter, concreteness, partialSymbol, mustResultSet);
		} else {
			return new FourValuedInterpretation(
					adapter, concreteness, partialSymbol, mayResultSet, mustResultSet);
		}
	}

	@Override
	public void configure(ModelStoreBuilder storeBuilder, Set<Concreteness> requiredInterpretations) {
		var queryBuilder = storeBuilder.getAdapter(ModelQueryBuilder.class);
		if (requiredInterpretations.contains(Concreteness.PARTIAL)) {
			queryBuilder.queries(may, must);
		}
		if (requiredInterpretations.contains(Concreteness.CANDIDATE)) {
			queryBuilder.queries(candidateMay, candidateMust);
		}
	}

	private static class TwoValuedInterpretation extends AbstractPartialInterpretation<TruthValueConfidence, Boolean> {
		private final ResultSet<Boolean> resultSet;

		protected TwoValuedInterpretation(
				ReasoningAdapter adapter, Concreteness concreteness, PartialSymbol<TruthValueConfidence, Boolean> partialSymbol,
				ResultSet<Boolean> resultSet) {
			super(adapter, concreteness, partialSymbol);
			this.resultSet = resultSet;
		}

		@Override
		public TruthValueConfidence get(Tuple key) {
			return new TruthValueConfidence(TruthValue.toTruthValue(resultSet.get(key)),
					Boolean.TRUE.equals(resultSet.get(key)) ? 1.0 : 0.0);
		}

		@Override
		public Cursor<Tuple, TruthValueConfidence> getAll() {
			return new TwoValuedCursor(resultSet.getAll());
		}

		private record TwoValuedCursor(Cursor<Tuple, Boolean> cursor) implements Cursor<Tuple, TruthValueConfidence> {
			@Override
			public Tuple getKey() {
				return cursor.getKey();
			}

			@Override
			public TruthValueConfidence getValue() {
				return new TruthValueConfidence(TruthValue.toTruthValue(cursor.getValue()),
						Boolean.TRUE.equals(cursor.getValue()) ? 1.0 : 0.0);
			}

			@Override
			public boolean isTerminated() {
				return cursor.isTerminated();
			}

			@Override
			public boolean move() {
				return cursor.move();
			}
		}
	}

	private static class FourValuedInterpretation extends AbstractPartialInterpretation<TruthValueConfidence, Boolean> {
		private final ResultSet<Boolean> mayResultSet;
		private final ResultSet<Boolean> mustResultSet;

		public FourValuedInterpretation(
				ReasoningAdapter adapter, Concreteness concreteness, PartialSymbol<TruthValueConfidence, Boolean> partialSymbol,
				ResultSet<Boolean> mayResultSet, ResultSet<Boolean> mustResultSet) {
			super(adapter, concreteness, partialSymbol);
			this.mayResultSet = mayResultSet;
			this.mustResultSet = mustResultSet;
		}

		@Override
		public TruthValueConfidence get(Tuple key) {
			// TODO: This is definitely incorrect
			boolean isMay = mayResultSet.get(key);
			boolean isMust = mustResultSet.get(key);
			var interpretation = this.getAdapter().getPartialInterpretation(Concreteness.PARTIAL, this.getPartialSymbol());
			if (isMust) {
				return new TruthValueConfidence(isMay ? TruthValue.TRUE : TruthValue.ERROR, 1.0);
			} else {
				return isMay ? new TruthValueConfidence(TruthValue.UNKNOWN, interpretation.get(key).getConfidence())
						: new TruthValueConfidence(TruthValue.FALSE, 0.0);
			}
		}

		@Override
		public Cursor<Tuple, TruthValueConfidence> getAll() {
			return new FourValuedCursor();
		}

		private final class FourValuedCursor implements Cursor<Tuple, TruthValueConfidence> {
			private final Cursor<Tuple, Boolean> mayCursor;
			private Cursor<Tuple, Boolean> mustCursor;

			private FourValuedCursor() {
				this.mayCursor = mayResultSet.getAll();
			}

			@Override
			public Tuple getKey() {
				return mustCursor == null ? mayCursor.getKey() : mustCursor.getKey();
			}

			@Override
			public TruthValueConfidence getValue() {
				if (mustCursor != null) {
					return new TruthValueConfidence(TruthValue.ERROR, 1.0);
				}
				if (Boolean.TRUE.equals(mustResultSet.get(mayCursor.getKey()))) {
					return new TruthValueConfidence(TruthValue.TRUE, 1.0);
				}
				return new TruthValueConfidence(TruthValue.UNKNOWN, 0.5);
			}

			@Override
			public boolean isTerminated() {
				return mustCursor != null && mustCursor.isTerminated();
			}

			@Override
			public boolean move() {
				if (mayCursor.isTerminated()) {
					return moveMust();
				}
				if (mayCursor.move()) {
					return true;
				}
				mustCursor = mustResultSet.getAll();
				return moveMust();
			}

			private boolean moveMust() {
				while (mustCursor.move()) {
					// We already iterated over {@code TRUE} truth values with {@code mayCursor}.
					if (!Boolean.TRUE.equals(mayResultSet.get(mustCursor.getKey()))) {
						return true;
					}
				}
				return false;
			}
		}
	}
}
