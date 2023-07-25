/*
 * SPDX-FileCopyrightText: 2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.reasoning.translator.multiobject;

import tools.refinery.store.model.ModelStoreBuilder;
import tools.refinery.store.model.ModelStoreConfiguration;
import tools.refinery.store.query.dnf.Query;
import tools.refinery.store.query.view.AnySymbolView;
import tools.refinery.store.reasoning.ReasoningAdapter;
import tools.refinery.store.reasoning.ReasoningBuilder;
import tools.refinery.store.reasoning.representation.PartialFunction;
import tools.refinery.store.reasoning.translator.PartialRelationTranslator;
import tools.refinery.store.reasoning.translator.RoundingMode;
import tools.refinery.store.representation.Symbol;
import tools.refinery.store.representation.cardinality.CardinalityDomain;
import tools.refinery.store.representation.cardinality.CardinalityInterval;
import tools.refinery.store.representation.cardinality.UpperCardinalities;
import tools.refinery.store.representation.cardinality.UpperCardinality;

import java.util.List;

import static tools.refinery.store.query.literal.Literals.check;
import static tools.refinery.store.query.term.int_.IntTerms.constant;
import static tools.refinery.store.query.term.int_.IntTerms.greaterEq;
import static tools.refinery.store.query.term.uppercardinality.UpperCardinalityTerms.constant;
import static tools.refinery.store.query.term.uppercardinality.UpperCardinalityTerms.greaterEq;

public class MultiObjectTranslator implements ModelStoreConfiguration {
	private static final Symbol<CardinalityInterval> COUNT_STORAGE = Symbol.of("COUNT", 1, CardinalityInterval.class,
			null);
	public static final AnySymbolView LOWER_CARDINALITY_VIEW = new LowerCardinalityView(COUNT_STORAGE);
	public static final AnySymbolView UPPER_CARDINALITY_VIEW = new UpperCardinalityView(COUNT_STORAGE);
	public static final PartialFunction<CardinalityInterval, Integer> COUNT_SYMBOL = new PartialFunction<>("COUNT", 1,
			CardinalityDomain.INSTANCE);

	@Override
	public void apply(ModelStoreBuilder storeBuilder) {
		storeBuilder.symbol(COUNT_STORAGE);

		storeBuilder.with(PartialRelationTranslator.of(ReasoningAdapter.EXISTS_SYMBOL)
				.may(Query.of("MAY_EXISTS", (builder, p1) -> builder
						.clause(UpperCardinality.class, upper -> List.of(
								UPPER_CARDINALITY_VIEW.call(p1, upper),
								check(greaterEq(upper, constant(UpperCardinalities.ONE)))
						))))
				.must(Query.of("MUST_EXISTS", (builder, p1) -> builder
						.clause(Integer.class, lower -> List.of(
								LOWER_CARDINALITY_VIEW.call(p1, lower),
								check(greaterEq(lower, constant(1)))
						))))
				.roundingMode(RoundingMode.PREFER_FALSE)
				.refiner(ExistsRefiner.of(COUNT_STORAGE)));

		storeBuilder.with(PartialRelationTranslator.of(ReasoningAdapter.EQUALS_SYMBOL)
				.rewriter(EqualsRelationRewriter.of(UPPER_CARDINALITY_VIEW))
				.refiner(EqualsRefiner.of(COUNT_STORAGE)));

		var reasoningBuilder = storeBuilder.getAdapter(ReasoningBuilder.class);
		reasoningBuilder.initializer(new MultiObjectInitializer(COUNT_STORAGE));
		reasoningBuilder.storageRefiner(COUNT_STORAGE, MultiObjectStorageRefiner::new);
	}
}
