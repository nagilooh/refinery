/*
 * SPDX-FileCopyrightText: 2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.reasoning.translator.metamodel;

import tools.refinery.logic.term.truthvalue.TruthValueConfidence;
import tools.refinery.store.reasoning.representation.ConfidencePartialRelation;
import tools.refinery.store.reasoning.representation.PartialRelation;
import tools.refinery.store.reasoning.translator.ConcretizationSettings;
import tools.refinery.store.reasoning.translator.multiplicity.Multiplicity;

import java.util.Set;

public record ConfidenceReferenceInfo(boolean containment, PartialRelation sourceType, Multiplicity multiplicity,
                                      PartialRelation targetType, ConfidencePartialRelation opposite,
									  TruthValueConfidence defaultValue,
                                      ConcretizationSettings concretizationSettings,
									  Set<PartialRelation> supersets,
									  PartialRelation partialRelation) {
	public ConfidenceReferenceInfo {
		if (containment && !concretizationSettings.concretize()) {
			throw new IllegalArgumentException("Containment references must be concretized");
		}
	}

	public static ConfidenceReferenceInfoBuilder builder() {
		return new ConfidenceReferenceInfoBuilder();
	}
}
