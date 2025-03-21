/*
 * SPDX-FileCopyrightText: 2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.reasoning.translator.crossreference;

import tools.refinery.logic.term.truthvalue.TruthValue;
import tools.refinery.logic.term.truthvalue.TruthValueConfidence;
import tools.refinery.store.reasoning.ReasoningAdapter;
import tools.refinery.store.reasoning.refinement.ConcreteRelationConfidenceRefiner;
import tools.refinery.store.reasoning.refinement.TypeConstraintRefiner;
import tools.refinery.store.reasoning.representation.ConfidencePartialRelation;
import tools.refinery.store.reasoning.representation.PartialRelation;
import tools.refinery.store.reasoning.representation.PartialSymbol;
import tools.refinery.store.reasoning.seed.ModelSeed;
import tools.refinery.store.reasoning.translator.RoundingMode;
import tools.refinery.store.representation.Symbol;
import tools.refinery.store.tuple.Tuple;

import java.util.Set;

class DirectedCrossReferenceConfidenceRefiner extends ConcreteRelationConfidenceRefiner {
	private final ConfidencePartialRelation confidenceLinkType;
	private final PartialRelation sourceType;
	private final PartialRelation targetType;
	private final Set<PartialRelation> supersets;
	private final Set<PartialRelation> oppositeSupersets;
	private TypeConstraintRefiner typeConstraintRefiner;

	protected DirectedCrossReferenceConfidenceRefiner(
			ReasoningAdapter adapter, PartialSymbol<TruthValue, Boolean> partialSymbol,
			Symbol<TruthValueConfidence> concreteSymbol, DirectedCrossReferenceConfidenceInfo info, RoundingMode roundingMode, ConfidencePartialRelation confidenceLinkType) {
		super(adapter, partialSymbol, concreteSymbol, roundingMode);
		this.sourceType = info.sourceType();
		this.targetType = info.targetType();
		this.supersets = info.supersets();
		this.oppositeSupersets = info.oppositeSupersets();
		this.confidenceLinkType = confidenceLinkType;
	}

	@Override
	public void afterCreate() {
		var adapter = getAdapter();
		typeConstraintRefiner = new TypeConstraintRefiner(adapter, sourceType, targetType, supersets,
				oppositeSupersets);
	}

	@Override
	public boolean merge(Tuple key, TruthValue value) {
		if (!super.merge(key, value)) {
			return false;
		}
		if (value.must()) {
			return typeConstraintRefiner.merge(key);
		}
		return true;
	}

	@Override
	public void afterInitialize(ModelSeed modelSeed) {
		var linkType = getPartialSymbol();
		var cursor = modelSeed.getCursor(confidenceLinkType);
		while (cursor.move()) {
			var value = cursor.getValue();
			if (value.must()) {
				var key = cursor.getKey();
				if (!typeConstraintRefiner.merge(key)) {
					throw new IllegalArgumentException("Failed to merge type constraints of %s for key %s"
							.formatted(linkType, key));
				}
			}
		}
	}

	public static Factory<TruthValue, Boolean> of(Symbol<TruthValueConfidence> concreteSymbol,
										 DirectedCrossReferenceConfidenceInfo info,
												  RoundingMode roundingMode, ConfidencePartialRelation confidenceLinkType) {
		return (adapter, partialSymbol) -> new DirectedCrossReferenceConfidenceRefiner(adapter, partialSymbol,
				concreteSymbol, info, roundingMode, confidenceLinkType);
	}
}
