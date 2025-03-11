/*
 * SPDX-FileCopyrightText: 2023-2024 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.reasoning.translator.crossreference;

import tools.refinery.logic.dnf.Dnf;
import tools.refinery.logic.dnf.Query;
import tools.refinery.logic.dnf.RelationalQuery;
import tools.refinery.logic.term.truthvalue.TruthValue;
import tools.refinery.logic.term.truthvalue.TruthValueConfidence;
import tools.refinery.store.dse.propagation.PropagationBuilder;
import tools.refinery.store.dse.transition.Rule;
import tools.refinery.store.model.ModelStoreBuilder;
import tools.refinery.store.model.ModelStoreConfiguration;
import tools.refinery.store.reasoning.ReasoningAdapter;
import tools.refinery.store.reasoning.representation.ConfidencePartialRelation;
import tools.refinery.store.reasoning.representation.PartialRelation;
import tools.refinery.store.reasoning.translator.*;
import tools.refinery.store.reasoning.translator.multiplicity.ConfidenceInvalidMultiplicityErrorTranslator;
import tools.refinery.store.reasoning.translator.multiplicity.Multiplicity;
import tools.refinery.store.representation.Symbol;

import static tools.refinery.logic.literal.Literals.not;
import static tools.refinery.store.reasoning.actions.PartialActionLiterals.add;
import static tools.refinery.store.reasoning.actions.PartialActionLiterals.remove;
import static tools.refinery.store.reasoning.literal.PartialLiterals.*;
import static tools.refinery.store.reasoning.translator.multiobject.MultiObjectTranslator.MULTI_VIEW;

public class DirectedCrossReferenceConfidenceTranslator implements ModelStoreConfiguration {
	private final ConfidencePartialRelation linkType;
	private final DirectedCrossReferenceConfidenceInfo info;
	private final Symbol<TruthValueConfidence> confidenceSymbol;

	public DirectedCrossReferenceConfidenceTranslator(ConfidencePartialRelation linkType,
													  DirectedCrossReferenceConfidenceInfo info) {
		this.linkType = linkType;
		this.info = info;
		confidenceSymbol = Symbol.of(linkType.name(), 2, TruthValueConfidence.class, info.defaultValue());
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
		// TODO: Create translator for the derived PartialRelation without confidence value here? Or should
		//  ConfidencePartialRelationTranslator take care of it?
		var translator = ConfidencePartialRelationTranslator.of(linkType, info.partialRelation());
		translator.symbol(confidenceSymbol);
		if (defaultValue.may()) {
			throw new TranslationException(linkType,
					"Unsupported default value %s for directed cross reference %s".formatted(defaultValue, linkType));
		} else {
			configureWithDefaultFalse(storeBuilder);
		}
		var roundingMode = info.concretizationSettings().concretize() ? RoundingMode.PREFER_FALSE : RoundingMode.NONE;
		translator.refiner(DirectedCrossReferenceConfidenceRefiner.of(confidenceSymbol, info, roundingMode));
		translator.roundingMode(roundingMode);
		if (info.concretizationSettings().decide()) {
			translator.decision(Rule.of(linkType.name(), (builder, source, target) -> builder
					.clause(
							may(linkType.call(source, target)),
							not(candidateMust(linkType.call(source, target))),
							not(MULTI_VIEW.call(source)),
							not(MULTI_VIEW.call(target))
					)
					.action(
							add(linkType, source, target)
					)));
		}
		storeBuilder.with(translator);
		storeBuilder.with(new ConfidenceInvalidMultiplicityErrorTranslator(sourceType, linkType, false,
				info.sourceMultiplicity()));
		storeBuilder.with(new ConfidenceInvalidMultiplicityErrorTranslator(targetType, linkType, true,
				info.targetMultiplicity()));
	}


	private RelationalQuery createMayHelper(PartialRelation type, Multiplicity multiplicity, boolean inverse) {
		return CrossReferenceUtils.createMayHelper(linkType, type, multiplicity, inverse);
	}

	private RelationalQuery createCandidateMayHelper(PartialRelation type, Multiplicity multiplicity,
													 boolean inverse) {
		return CrossReferenceUtils.createCandidateMayHelper(linkType, type, multiplicity, inverse);
	}

	private Dnf createSupersetHelper() {
		return TranslatorUtils.createSupersetHelper(linkType, info.supersets(), info.oppositeSupersets());
	}

	private void configureWithDefaultFalse(ModelStoreBuilder storeBuilder) {
		var name = linkType.name();
		var sourceType = info.sourceType();
		var targetType = info.targetType();
		var mayNewSource = createMayHelper(sourceType, info.sourceMultiplicity(), false);
		var mayNewTarget = createMayHelper(targetType, info.targetMultiplicity(), true);
		var superset = createSupersetHelper();
		// Fail if there is no {@link PropagationBuilder}, since it is required for soundness.
		var propagationBuilder = storeBuilder.getAdapter(PropagationBuilder.class);
		propagationBuilder.rule(Rule.of(name + "#invalidLink", (builder, p1, p2) -> {
			builder.clause(
					may(linkType.call(p1, p2)),
					not(may(sourceType.call(p1)))
			);
			builder.clause(
					may(linkType.call(p1, p2)),
					not(may(targetType.call(p2)))
			);
			builder.clause(
					may(linkType.call(p1, p2)),
					not(may(superset.call(p1, p2)))
			);
			if (info.isConstrained()) {
				builder.clause(
						may(linkType.call(p1, p2)),
						not(must(linkType.call(p1, p2))),
						not(mayNewSource.call(p1))
				);
				builder.clause(
						may(linkType.call(p1, p2)),
						not(must(linkType.call(p1, p2))),
						not(mayNewTarget.call(p2))
				);
			}
			builder.action(
					remove(linkType, p1, p2)
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
							candidateMay(linkType.call(p1, p2)),
							not(candidateMay(sourceType.call(p1)))
					)
					.clause(
							candidateMay(linkType.call(p1, p2)),
							not(candidateMay(targetType.call(p2)))
					)
					.clause(
							candidateMay(linkType.call(p1, p2)),
							not(candidateMay(superset.call(p1, p2)))
					);
			if (info.isConstrained()) {
				queryBuilder.clause(
						candidateMay(linkType.call(p1, p2)),
						not(candidateMust(linkType.call(p1, p2))),
						not(candidateMayNewSource.call(p1))
				);
				queryBuilder.clause(
						candidateMay(linkType.call(p1, p2)),
						not(candidateMust(linkType.call(p1, p2))),
						not(candidateMayNewTarget.call(p2))
				);
			}
			builder.clause(
					queryBuilder.build().call(p1, p2),
					candidateMust(ReasoningAdapter.EXISTS_SYMBOL.call(p1)),
					candidateMust(ReasoningAdapter.EXISTS_SYMBOL.call(p2))
			);
			builder.action(
					remove(linkType, p1, p2)
			);
		}));
	}

}
