/*
 * SPDX-FileCopyrightText: 2023-2024 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.reasoning.translator.metamodel;

import tools.refinery.store.reasoning.representation.AnyPartialSymbol;
import tools.refinery.store.reasoning.representation.ConfidencePartialRelation;
import tools.refinery.store.reasoning.representation.PartialRelation;
import tools.refinery.store.reasoning.translator.ConcretizationSettings;
import tools.refinery.store.reasoning.translator.TranslationException;
import tools.refinery.store.reasoning.translator.containment.ContainmentHierarchyTranslator;
import tools.refinery.store.reasoning.translator.containment.ContainmentInfo;
import tools.refinery.store.reasoning.translator.crossreference.DirectedCrossReferenceConfidenceInfo;
import tools.refinery.store.reasoning.translator.crossreference.DirectedCrossReferenceInfo;
import tools.refinery.store.reasoning.translator.crossreference.UndirectedCrossReferenceInfo;
import tools.refinery.store.reasoning.translator.multiplicity.Multiplicity;
import tools.refinery.store.reasoning.translator.multiplicity.UnconstrainedMultiplicity;
import tools.refinery.store.reasoning.translator.typehierarchy.TypeInfo;

import java.util.*;
import java.util.function.Consumer;

public class ConfidenceMetamodelBuilder {
	private final ContainedTypeHierarchyBuilder typeHierarchyBuilder = new ContainedTypeHierarchyBuilder();
	private final Map<PartialRelation, ReferenceInfo> referenceInfoMap = new LinkedHashMap<>();
	private final Map<PartialRelation, ReferenceInfo> directedReferenceInfoMap =
			new LinkedHashMap<>();
	private final Map<ConfidencePartialRelation, ConfidenceReferenceInfo> directedConfidenceReferenceInfoMap =
			new LinkedHashMap<>();
	private final Set<PartialRelation> containerTypes = new HashSet<>();
	private final Set<PartialRelation> containedTypes = new HashSet<>();
	private final Map<PartialRelation, ContainmentInfo> containmentHierarchy = new LinkedHashMap<>();
	private final Map<PartialRelation, DirectedCrossReferenceInfo> directedCrossReferences =
			new LinkedHashMap<>();
	private final Map<ConfidencePartialRelation, DirectedCrossReferenceConfidenceInfo> directedConfidenceCrossReferences =
			new LinkedHashMap<>();
	private final Map<ConfidencePartialRelation, PartialRelation> directedConfidencePartialRelations =
			new LinkedHashMap<>();
	private final Map<PartialRelation, UndirectedCrossReferenceInfo> undirectedCrossReferences = new LinkedHashMap<>();
	private final Map<PartialRelation, PartialRelation> oppositeReferences = new LinkedHashMap<>();
	private final Map<ConfidencePartialRelation, ConfidencePartialRelation> directedOppositeReferences =
			new LinkedHashMap<>();

	ConfidenceMetamodelBuilder() {
		typeHierarchyBuilder.type(ContainmentHierarchyTranslator.CONTAINER_SYMBOL, true);
		typeHierarchyBuilder.type(ContainmentHierarchyTranslator.CONTAINED_SYMBOL, true);
	}

	public ConfidenceMetamodelBuilder type(PartialRelation partialRelation, TypeInfo typeInfo) {
		typeHierarchyBuilder.type(partialRelation, typeInfo);
		return this;

	}

	public ConfidenceMetamodelBuilder type(PartialRelation partialRelation, boolean abstractType,
                                           PartialRelation... supertypes) {
		typeHierarchyBuilder.type(partialRelation, abstractType, supertypes);
		return this;
	}

	public ConfidenceMetamodelBuilder type(PartialRelation partialRelation, boolean abstractType,
                                           Collection<PartialRelation> supertypes) {
		typeHierarchyBuilder.type(partialRelation, abstractType, supertypes);
		return this;
	}

	public ConfidenceMetamodelBuilder type(PartialRelation partialRelation, PartialRelation... supertypes) {
		typeHierarchyBuilder.type(partialRelation, supertypes);
		return this;
	}

	public ConfidenceMetamodelBuilder type(PartialRelation partialRelation, Collection<PartialRelation> supertypes) {
		typeHierarchyBuilder.type(partialRelation, supertypes);
		return this;
	}

	public ConfidenceMetamodelBuilder types(Collection<Map.Entry<PartialRelation, TypeInfo>> entries) {
		typeHierarchyBuilder.types(entries);
		return this;
	}

	public ConfidenceMetamodelBuilder types(Map<PartialRelation, TypeInfo> map) {
		typeHierarchyBuilder.types(map);
		return this;
	}

	public ConfidenceMetamodelBuilder reference(PartialRelation linkType,
												Consumer<ReferenceInfoBuilder> callback) {
		var builder = ReferenceInfo.builder();
		callback.accept(builder);
		return reference(linkType, builder.build());
	}

	public ConfidenceMetamodelBuilder reference(PartialRelation linkType, ReferenceInfo info) {
		if (linkType.arity() != 2) {
			throw new TranslationException(linkType,
					"Only references of arity 2 are supported, got %s with %d instead".formatted(
							linkType, linkType.arity()));
		}
		var putResult = referenceInfoMap.put(linkType, info);
		if (putResult != null && !putResult.equals(info)) {
			throw new TranslationException(linkType, "Duplicate reference info for partial relation: " + linkType);
		}
		return this;
	}

	public ConfidenceMetamodelBuilder references(Collection<Map.Entry<PartialRelation, ReferenceInfo>> entries) {
		for (var entry : entries) {
			reference(entry.getKey(), entry.getValue());
		}
		return this;
	}

	public ConfidenceMetamodelBuilder references(Map<PartialRelation, ReferenceInfo> map) {
		return references(map.entrySet());
	}

	public ConfidenceMetamodelBuilder directedReference(ConfidencePartialRelation linkType,
												Consumer<ConfidenceReferenceInfoBuilder> callback) {
		var builder = ConfidenceReferenceInfo.builder();
		callback.accept(builder);
		return directedReference(linkType, builder.build());
	}

	public ConfidenceMetamodelBuilder directedReference(ConfidencePartialRelation linkType, ConfidenceReferenceInfo info) {
		if (linkType.arity() != 2) {
			throw new TranslationException(linkType,
					"Only references of arity 2 are supported, got %s with %d instead".formatted(
							linkType, linkType.arity()));
		}
		var putResult = directedConfidenceReferenceInfoMap.put(linkType, info);
		if (putResult != null && !putResult.equals(info)) {
			throw new TranslationException(linkType, "Duplicate reference info for partial relation: " + linkType);
		}
		return this;
	}

	public ConfidenceMetamodelBuilder directedReference(Collection<Map.Entry<ConfidencePartialRelation, ConfidenceReferenceInfo>> entries) {
		for (var entry : entries) {
			directedReference(entry.getKey(), entry.getValue());
		}
		return this;
	}

	public ConfidenceMetamodelBuilder directedReference(Map<ConfidencePartialRelation, ConfidenceReferenceInfo> map) {
		return directedReference(map.entrySet());
	}

	public ConfidenceMetamodel build() {
		for (var entry : referenceInfoMap.entrySet()) {
			var linkType = entry.getKey();
			var info = entry.getValue();
			processReferenceInfo(linkType, info);
		}
		for (var entry : directedReferenceInfoMap.entrySet()) {
			var linkType = entry.getKey();
			var info = entry.getValue();
			processReferenceInfo(linkType, info);
		}
		for (var entry : directedConfidenceReferenceInfoMap.entrySet()) {
			var linkType = entry.getKey();
			var info = entry.getValue();
			processConfidenceReferenceInfo(linkType, info);
		}
		typeHierarchyBuilder.setContainerTypes(containerTypes);
		typeHierarchyBuilder.setContainedTypes(containedTypes);
		var typeHierarchy = typeHierarchyBuilder.build();
		return new ConfidenceMetamodel(typeHierarchy, Collections.unmodifiableMap(containmentHierarchy),
				Collections.unmodifiableMap(directedCrossReferences),
				Collections.unmodifiableMap(directedConfidenceCrossReferences),
				Collections.unmodifiableMap(directedConfidencePartialRelations),
				Collections.unmodifiableMap(undirectedCrossReferences),
				Collections.unmodifiableMap(oppositeReferences),
				Collections.unmodifiableMap(directedOppositeReferences));
	}

	private void processReferenceInfo(PartialRelation linkType, ReferenceInfo info) {
		if (oppositeReferences.containsKey(linkType) || containmentHierarchy.containsKey(linkType)) {
			// We already processed this reference while processing its opposite.
			return;
		}
		var sourceType = info.sourceType();
		if (typeHierarchyBuilder.isInvalidType(sourceType)) {
			throw new TranslationException(linkType, "Source type %s of %s is not in type hierarchy"
					.formatted(sourceType, linkType));
		}
		var targetType = info.targetType();
		var opposite = info.opposite();
		Multiplicity targetMultiplicity = UnconstrainedMultiplicity.INSTANCE;
		var defaultValue = info.defaultValue();
		Set<PartialRelation> oppositeSupersets = Set.of();
		if (opposite != null) {
			var oppositeInfo = referenceInfoMap.get(opposite);
			validateOpposite(linkType, info, opposite, oppositeInfo);
			targetMultiplicity = oppositeInfo.multiplicity();
			defaultValue = defaultValue.meet(oppositeInfo.defaultValue());
			if (oppositeInfo.containment()) {
				// Skip processing this reference and process it once we encounter its containment opposite.
				return;
			}
			if (opposite.equals(linkType)) {
				if (!sourceType.equals(targetType)) {
					throw new TranslationException(linkType,
							"Target %s of undirected reference %s differs from source %s".formatted(
									targetType, linkType, sourceType));
				}
				undirectedCrossReferences.put(linkType, new UndirectedCrossReferenceInfo(sourceType,
						info.multiplicity(), defaultValue, info.concretizationSettings(), info.supersets()));
				return;
			}
			oppositeReferences.put(opposite, linkType);
			oppositeSupersets = oppositeInfo.supersets();
		}
		if (info.containment()) {
			processContainmentInfo(linkType, info, targetMultiplicity);
			return;
		}
		directedCrossReferences.put(linkType, new DirectedCrossReferenceInfo(sourceType, info.multiplicity(),
				targetType, targetMultiplicity, defaultValue, info.concretizationSettings(), info.supersets(),
				oppositeSupersets));
	}

	private void processConfidenceReferenceInfo(ConfidencePartialRelation linkType, ConfidenceReferenceInfo info) {
		if (directedOppositeReferences.containsKey(linkType)) {
			// We already processed this reference while processing its opposite.
			return;
		}
		var sourceType = info.sourceType();
		if (typeHierarchyBuilder.isInvalidType(sourceType)) {
			throw new TranslationException(linkType, "Source type %s of %s is not in type hierarchy"
					.formatted(sourceType, linkType));
		}
		var targetType = info.targetType();
		var opposite = info.opposite();
		var partialRelation = info.partialRelation();
		if (partialRelation == null) {
			throw new TranslationException(linkType, "Partial relation of %s is null".formatted(linkType));
		}
		Multiplicity targetMultiplicity = UnconstrainedMultiplicity.INSTANCE;
		var defaultValue = info.defaultValue();
		Set<PartialRelation> oppositeSupersets = Set.of();
		if (opposite != null) {
			var oppositeInfo = directedConfidenceReferenceInfoMap.get(opposite);
			validateDirectedOpposite(linkType, info, opposite, oppositeInfo);
			targetMultiplicity = oppositeInfo.multiplicity();
			defaultValue = defaultValue.meet(oppositeInfo.defaultValue());
			if (oppositeInfo.containment()) {
				throw new TranslationException(linkType,
						"Reference %s with confidence value cannot be the opposite of containment".formatted(linkType));
			}
			if (opposite.equals(linkType)) {
				throw new TranslationException(linkType,
						"Reference %s with confidence value cannot be undirected".formatted(linkType));
			}
			directedOppositeReferences.put(opposite, linkType);
			oppositeSupersets = oppositeInfo.supersets();
		}
		if (info.containment()) {
			throw new TranslationException(linkType,
					"Reference %s with confidence value cannot be containment".formatted(linkType));
		}
		directedConfidenceCrossReferences.put(linkType, new DirectedCrossReferenceConfidenceInfo(sourceType, info.multiplicity(),
				targetType, targetMultiplicity, defaultValue, info.concretizationSettings(), info.supersets(),
				oppositeSupersets, partialRelation));
		directedConfidencePartialRelations.put(linkType, partialRelation);
	}

	private void processContainmentInfo(PartialRelation linkType, ReferenceInfo info,
										Multiplicity targetMultiplicity) {
		var sourceType = info.sourceType();
		var targetType = info.targetType();
		var opposite = info.opposite();
		if (typeHierarchyBuilder.isInvalidType(targetType)) {
			throw new TranslationException(linkType, "Target type %s of %s is not in type hierarchy"
					.formatted(targetType, linkType));
		}
		if (!UnconstrainedMultiplicity.INSTANCE.equals(targetMultiplicity)) {
			throw new TranslationException(opposite, "Invalid opposite %s with multiplicity %s of containment %s"
					.formatted(opposite, targetMultiplicity, linkType));
		}
		containerTypes.add(sourceType);
		containedTypes.add(targetType);
		containmentHierarchy.put(linkType, new ContainmentInfo(sourceType, info.multiplicity(), targetType,
                info.concretizationSettings().decide(), info.supersets(),
				info.opposite() == null ? new LinkedHashSet<>() : referenceInfoMap.get(opposite).supersets()));
	}

	private static void validateOpposite(PartialRelation linkType, ReferenceInfo info, PartialRelation opposite,
										 ReferenceInfo oppositeInfo) {
		var sourceType = info.sourceType();
		var targetType = info.targetType();
		var containment = oppositeInfo.containment();
		var concretizationSettings = info.concretizationSettings();
		var oppositeSourceType = oppositeInfo.sourceType();
		var oppositeTargetType = oppositeInfo.targetType();
		var oppositeOpposite = oppositeInfo.opposite();
		var oppositeContainment = oppositeInfo.containment();
		var oppositeConcretizationSettings = oppositeInfo.concretizationSettings();

		validateAnyOpposite(linkType, info, opposite, oppositeInfo, sourceType, targetType, containment,
				concretizationSettings, oppositeSourceType, oppositeTargetType, oppositeOpposite, oppositeContainment,
				oppositeConcretizationSettings);
	}

	private static void validateDirectedOpposite(ConfidencePartialRelation linkType, ConfidenceReferenceInfo info,
												 ConfidencePartialRelation opposite,
												 ConfidenceReferenceInfo oppositeInfo) {
		var sourceType = info.sourceType();
		var targetType = info.targetType();
		var containment = oppositeInfo.containment();
		var concretizationSettings = info.concretizationSettings();
		var oppositeSourceType = oppositeInfo.sourceType();
		var oppositeTargetType = oppositeInfo.targetType();
		var oppositeOpposite = oppositeInfo.opposite();
		var oppositeContainment = oppositeInfo.containment();
		var oppositeConcretizationSettings = oppositeInfo.concretizationSettings();

		validateAnyOpposite(linkType, info, opposite, oppositeInfo, sourceType, targetType, containment,
				concretizationSettings, oppositeSourceType, oppositeTargetType, oppositeOpposite, oppositeContainment,
				oppositeConcretizationSettings);
	}

	private static void validateAnyOpposite(AnyPartialSymbol linkType, Object info, AnyPartialSymbol opposite,
											Object oppositeInfo, PartialRelation sourceType,
											PartialRelation targetType,
											boolean containment,
											ConcretizationSettings concretizationSettings,
											PartialRelation oppositeSourceType, PartialRelation oppositeTargetType,
											AnyPartialSymbol oppositeOpposite, boolean oppositeContainment,
											ConcretizationSettings oppositeConcretizationSettings) {
		if (oppositeInfo == null) {
			throw new TranslationException(linkType, "Opposite %s of %s is not defined"
					.formatted(opposite, linkType));
		}
		if (!linkType.equals(oppositeOpposite)) {
			throw new TranslationException(opposite, "Expected %s to have opposite %s, got %s instead"
					.formatted(opposite, linkType, oppositeOpposite));
		}
		if (!targetType.equals(oppositeSourceType)) {
			throw new TranslationException(linkType, "Expected %s to have source type %s, got %s instead"
					.formatted(opposite, targetType, oppositeSourceType));
		}
		if (!sourceType.equals(oppositeTargetType)) {
			throw new TranslationException(linkType, "Expected %s to have target type %s, got %s instead"
					.formatted(opposite, sourceType, oppositeTargetType));
		}
		if (oppositeContainment && containment) {
			throw new TranslationException(opposite, "Opposite %s of containment %s cannot be containment"
					.formatted(opposite, linkType));
		}
		if (!concretizationSettings.equals(oppositeConcretizationSettings)) {
			throw new TranslationException(opposite, "Concretization settings of opposites %s and %s don't match"
					.formatted(opposite, linkType));
		}
	}
}
