/*
 * SPDX-FileCopyrightText: 2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.reasoning.translator.metamodel;

import tools.refinery.store.reasoning.representation.ConfidencePartialRelation;
import tools.refinery.store.reasoning.representation.PartialRelation;
import tools.refinery.store.reasoning.translator.containment.ContainmentInfo;
import tools.refinery.store.reasoning.translator.crossreference.DirectedCrossReferenceConfidenceInfo;
import tools.refinery.store.reasoning.translator.crossreference.DirectedCrossReferenceInfo;
import tools.refinery.store.reasoning.translator.crossreference.UndirectedCrossReferenceInfo;
import tools.refinery.store.reasoning.translator.typehierarchy.TypeHierarchy;

import java.util.Map;

public record ConfidenceMetamodel(TypeHierarchy typeHierarchy, Map<PartialRelation, ContainmentInfo> containmentHierarchy,
                                  Map<PartialRelation, DirectedCrossReferenceInfo> directedCrossReferences,
								  Map<ConfidencePartialRelation, DirectedCrossReferenceConfidenceInfo> directedConfidenceCrossReferences,
								  Map<ConfidencePartialRelation, PartialRelation> directedConfidencePartialRelations,
                                  Map<PartialRelation, UndirectedCrossReferenceInfo> undirectedCrossReferences,
                                  Map<PartialRelation, PartialRelation> oppositeReferences,
								  Map<ConfidencePartialRelation, ConfidencePartialRelation> directedOppositeReferences) {
	public static ConfidenceMetamodelBuilder builder() {
		return new ConfidenceMetamodelBuilder();
	}
}
