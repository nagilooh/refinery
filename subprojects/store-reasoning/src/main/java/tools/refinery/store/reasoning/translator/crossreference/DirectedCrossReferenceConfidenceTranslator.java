/*
 * SPDX-FileCopyrightText: 2023-2024 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.reasoning.translator.crossreference;

import tools.refinery.logic.dnf.Dnf;
import tools.refinery.logic.dnf.FunctionalQuery;
import tools.refinery.logic.dnf.Query;
import tools.refinery.logic.dnf.RelationalQuery;
import tools.refinery.logic.term.Variable;
import tools.refinery.logic.term.real.RealTerms;
import tools.refinery.logic.term.truthvalue.TruthValueConfidence;
import tools.refinery.store.dse.propagation.PropagationBuilder;
import tools.refinery.store.dse.transition.Rule;
import tools.refinery.store.model.ModelStoreBuilder;
import tools.refinery.store.model.ModelStoreConfiguration;
import tools.refinery.store.query.view.MayConfidenceView;
import tools.refinery.store.query.view.MustConfidenceView;
import tools.refinery.store.reasoning.ReasoningAdapter;
import tools.refinery.store.reasoning.representation.ConfidencePartialRelation;
import tools.refinery.store.reasoning.representation.PartialRelation;
import tools.refinery.store.reasoning.translator.*;
import tools.refinery.store.reasoning.translator.multiplicity.InvalidMultiplicityErrorTranslator;
import tools.refinery.store.reasoning.translator.multiplicity.Multiplicity;
import tools.refinery.store.representation.Symbol;

import java.util.List;

import static tools.refinery.logic.literal.Literals.not;
import static tools.refinery.logic.term.real.RealTerms.REAL_SUM;
import static tools.refinery.store.reasoning.actions.PartialActionLiterals.add;
import static tools.refinery.store.reasoning.actions.PartialActionLiterals.remove;
import static tools.refinery.store.reasoning.literal.PartialLiterals.*;
import static tools.refinery.store.reasoning.translator.multiobject.MultiObjectTranslator.MULTI_VIEW;

public class DirectedCrossReferenceConfidenceTranslator implements ModelStoreConfiguration {
	private final ConfidencePartialRelation linkType;
	private final DirectedCrossReferenceConfidenceInfo info;
	private final Symbol<TruthValueConfidence> confidenceSymbol;
	private final ConfidenceView confidenceView;
	private final FunctionalQuery<Double> upQuery;
	private final FunctionalQuery<Double> downQuery;
	private final FunctionalQuery<Double> currentQuery;

	public DirectedCrossReferenceConfidenceTranslator(ConfidencePartialRelation linkType,
													  DirectedCrossReferenceConfidenceInfo info) {
		this.linkType = linkType;
		this.info = info;
		confidenceSymbol = Symbol.of(linkType.name(), 2, TruthValueConfidence.class, info.defaultValue());
		confidenceView = new ConfidenceView(confidenceSymbol, linkType.name() + "#confidence");
		var upHelper = Query.of(linkType.name() + "#up#helper", Double.class, (builder, p1, p2, output) -> builder
				.clause(Double.class, d1 -> List.of(
						confidenceView.call(p1, p2, d1),
						output.assign(RealTerms.max(RealTerms.log(d1),
								RealTerms.log(RealTerms.sub(RealTerms.constant(1.0), d1))))
				)));
		upQuery = Query.of(linkType.name() + "#up", Double.class, (builder, output) -> builder
				.clause(
						output.assign(upHelper.aggregate(REAL_SUM, Variable.of(), Variable.of()))
				));
		var downHelper = Query.of(linkType.name() + "#down#helper", Double.class, (builder, p1, p2, output) -> builder
				.clause(Double.class, d1 -> List.of(
						confidenceView.call(p1, p2, d1),
						output.assign(RealTerms.min(RealTerms.log(d1), RealTerms.log(RealTerms.sub(RealTerms.constant(1.0), d1))))
				)));
		downQuery = Query.of(linkType.name() + "#down", Double.class, (builder, output) -> builder
				.clause(
						output.assign(downHelper.aggregate(REAL_SUM, Variable.of(), Variable.of()))
				));
		var currentHelper = Query.of(linkType.name() + "#current#helper", Double.class,
				(builder, p1, p2, output) -> builder
				.clause(Double.class, d1 -> List.of(
						confidenceView.call(p1, p2, d1),
						output.assign(RealTerms.log(RealTerms.sub(RealTerms.constant(1.0), d1)))
				)));
		currentQuery = Query.of(linkType.name() + "#current", Double.class, (builder, output) -> builder
				.clause(
						output.assign(currentHelper.aggregate(REAL_SUM, Variable.of(), Variable.of()))
				));

	}

	@Override
	public void apply(ModelStoreBuilder storeBuilder) {
		var sourceType = info.sourceType();
		var targetType = info.targetType();
		var defaultValue = info.defaultValue();
		if (defaultValue.must()) {
			throw new TranslationException(linkType, "Unsupported default value %s for directed cross reference %s"
					.formatted(defaultValue, linkType));
		}
		var partialRelation = info.partialRelation();
		var translator = PartialRelationTranslator.of(partialRelation)
				.may(Query.of(partialRelation.name() + "#may", (builder, p1, p2) -> builder
						.clause(
								new MayConfidenceView(confidenceSymbol).call(p1, p2)
						)))
				.must(Query.of(partialRelation.name() + "#must", (builder, p1, p2) -> builder
						.clause(
								new MustConfidenceView(confidenceSymbol).call(p1, p2)
						)));
		if (defaultValue.may()) {
			throw new TranslationException(linkType,
					"Unsupported default value %s for directed cross reference %s".formatted(defaultValue, linkType));
		} else {
			configureWithDefaultFalse(storeBuilder);
		}
		var roundingMode = info.concretizationSettings().concretize() ? RoundingMode.PREFER_FALSE : RoundingMode.NONE;
		translator.roundingMode(roundingMode);
		translator.refiner(DirectedCrossReferenceConfidenceRefiner.of(confidenceSymbol, info, roundingMode, linkType));
		if (info.concretizationSettings().decide()) {
			translator.decision(Rule.of(linkType.name(), (builder, source, target) -> builder
					.clause(
							may(partialRelation.call(source, target)),
							not(candidateMust(partialRelation.call(source, target))),
							not(MULTI_VIEW.call(source)),
							not(MULTI_VIEW.call(target))
					)
					.action(
							add(partialRelation, source, target)
					)));
		}
		storeBuilder.with(translator);
		storeBuilder.with(new InvalidMultiplicityErrorTranslator(sourceType, partialRelation, false,
				info.sourceMultiplicity()));
		storeBuilder.with(new InvalidMultiplicityErrorTranslator(targetType, partialRelation, true,
				info.targetMultiplicity()));
		storeBuilder.with(new ConfidencePartialRelationTranslator(linkType, roundingMode)
				.symbol(confidenceSymbol));


	}


	private RelationalQuery createMayHelper(PartialRelation type, Multiplicity multiplicity, boolean inverse) {
		return CrossReferenceUtils.createMayHelper(info.partialRelation(), type, multiplicity, inverse);
	}

	private RelationalQuery createCandidateMayHelper(PartialRelation type, Multiplicity multiplicity,
													 boolean inverse) {
		return CrossReferenceUtils.createCandidateMayHelper(info.partialRelation(), type, multiplicity, inverse);
	}

	private Dnf createSupersetHelper() {
		return TranslatorUtils.createSupersetHelper(info.partialRelation(), info.supersets(), info.oppositeSupersets());
	}

	private void configureWithDefaultFalse(ModelStoreBuilder storeBuilder) {
		var name = linkType.name();
		var sourceType = info.sourceType();
		var targetType = info.targetType();
		var mayNewSource = createMayHelper(sourceType, info.sourceMultiplicity(), false);
		var mayNewTarget = createMayHelper(targetType, info.targetMultiplicity(), true);
		var superset = createSupersetHelper();
		var partialRelation = info.partialRelation();
		// Fail if there is no {@link PropagationBuilder}, since it is required for soundness.
		var propagationBuilder = storeBuilder.getAdapter(PropagationBuilder.class);
		propagationBuilder.rule(Rule.of(name + "#invalidLink", (builder, p1, p2) -> {
			builder.clause(
					may(partialRelation.call(p1, p2)),
					not(may(sourceType.call(p1)))
			);
			builder.clause(
					may(partialRelation.call(p1, p2)),
					not(may(targetType.call(p2)))
			);
			builder.clause(
					may(partialRelation.call(p1, p2)),
					not(may(superset.call(p1, p2)))
			);
			if (info.isConstrained()) {
				builder.clause(
						may(partialRelation.call(p1, p2)),
						not(must(partialRelation.call(p1, p2))),
						not(mayNewSource.call(p1))
				);
				builder.clause(
						may(partialRelation.call(p1, p2)),
						not(must(partialRelation.call(p1, p2))),
						not(mayNewTarget.call(p2))
				);
			}
			builder.action(
					remove(partialRelation, p1, p2)
			);
		}));
		if (info.concretizationSettings().concretize()) {
			// References concretized by rounding down are already {@code false} in the candidate interpretation,
			// so we don't need to set them to {@code false} manually.
			return;
		}
		var candidateMayNewSource = createCandidateMayHelper(sourceType, info.sourceMultiplicity(), false);
		var candidateMayNewTarget = createCandidateMayHelper(targetType, info.targetMultiplicity(), true);
		propagationBuilder.concretizationRule(Rule.of(name + "#invalidLinkConcretization", (builder, p1, p2) -> {
			var queryBuilder = Query.builder(name + "#invalidLinkConcretizationPrecondition")
					.parameters(p1, p2)
					.clause(
							candidateMay(partialRelation.call(p1, p2)),
							not(candidateMay(sourceType.call(p1)))
					)
					.clause(
							candidateMay(partialRelation.call(p1, p2)),
							not(candidateMay(targetType.call(p2)))
					)
					.clause(
							candidateMay(partialRelation.call(p1, p2)),
							not(candidateMay(superset.call(p1, p2)))
					);
			if (info.isConstrained()) {
				queryBuilder.clause(
						candidateMay(partialRelation.call(p1, p2)),
						not(candidateMust(partialRelation.call(p1, p2))),
						not(candidateMayNewSource.call(p1))
				);
				queryBuilder.clause(
						candidateMay(partialRelation.call(p1, p2)),
						not(candidateMust(partialRelation.call(p1, p2))),
						not(candidateMayNewTarget.call(p2))
				);
			}
			builder.clause(
					queryBuilder.build().call(p1, p2),
					candidateMust(ReasoningAdapter.EXISTS_SYMBOL.call(p1)),
					candidateMust(ReasoningAdapter.EXISTS_SYMBOL.call(p2))
			);
			builder.action(
					remove(partialRelation, p1, p2)
			);
		}));
	}

	public FunctionalQuery<Double> getUpQuery() {
		return upQuery;
	}

	public FunctionalQuery<Double> getDownQuery() {
		return downQuery;
	}

	public FunctionalQuery<Double> getCurrentQuery() {
		return currentQuery;
	}
}
