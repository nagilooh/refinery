/*
 * SPDX-FileCopyrightText: 2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.reasoning.translator.containment;

import tools.refinery.logic.term.truthvalue.TruthValue;
import tools.refinery.store.model.Interpretation;
import tools.refinery.store.reasoning.ReasoningAdapter;
import tools.refinery.store.reasoning.refinement.AbstractPartialInterpretationRefiner;
import tools.refinery.store.reasoning.refinement.PartialInterpretationRefiner;
import tools.refinery.store.reasoning.refinement.RefinementUtils;
import tools.refinery.store.reasoning.refinement.TypeConstraintRefiner;
import tools.refinery.store.reasoning.representation.PartialRelation;
import tools.refinery.store.reasoning.representation.PartialSymbol;
import tools.refinery.store.reasoning.seed.ModelSeed;
import tools.refinery.store.representation.Symbol;
import tools.refinery.store.tuple.Tuple;

import java.util.ArrayList;
import java.util.Set;

class ContainmentLinkRefiner extends AbstractPartialInterpretationRefiner.ConcretizationAware<TruthValue, Boolean> {
	private final Factory factory;
	private final Interpretation<InferredContainment> interpretation;
	private TypeConstraintRefiner typeConstraintRefiner;

	private ContainmentLinkRefiner(ReasoningAdapter adapter, PartialSymbol<TruthValue, Boolean> partialSymbol,
								   Factory factory) {
		super(adapter, partialSymbol);
		this.factory = factory;
		interpretation = adapter.getModel().getInterpretation(factory.symbol);
	}

	@Override
	public void afterCreate() {
		var adapter = getAdapter();
		typeConstraintRefiner = new TypeConstraintRefiner(adapter, factory.sourceType, factory.targetType,
				factory.supersets, factory.oppositeSupersets);
	}

	@Override
	public boolean merge(Tuple key, TruthValue value) {
		var oldValue = interpretation.get(key);
		var newValue = mergeLink(oldValue, value);
		if (oldValue != newValue) {
			interpretation.put(key, newValue);
		}
		if (value.must()) {
			return typeConstraintRefiner.merge(key);
		}
		return true;
	}

	@Override
	public boolean join(Tuple key, TruthValue value) {
		if (concretizationInProgress()) {
			return false;
		}

		var oldValue = interpretation.get(key);
		var oldLinkValue = getLinkValue(oldValue);
		var joinedLinkValue = oldLinkValue.join(value);
		var mustLinks = removeFromSet(oldValue.mustLinks());
		var forbiddenLinks = removeFromSet(oldValue.forbiddenLinks());
		switch (joinedLinkValue) {
		case TRUE -> mustLinks = addToSet(mustLinks, factory.linkType);
		case FALSE -> forbiddenLinks = addToSet(forbiddenLinks, factory.linkType);
		case ERROR -> {
			mustLinks = addToSet(mustLinks, factory.linkType);
			forbiddenLinks = addToSet(forbiddenLinks, factory.linkType);
		}
		case UNKNOWN -> {
		}
		}

		var joinedContains = oldValue.contains().join(value);
		var newValue = new InferredContainment(joinedContains, mustLinks, forbiddenLinks);
		if (!oldValue.equals(newValue)) {
			interpretation.put(key, newValue);
			if (!oldValue.contains().equals(newValue.contains())) {
				return notifyAbstractionListeners(key, oldValue.contains(), newValue.contains());
			}
		}
		return true;
	}

	private TruthValue getLinkValue(InferredContainment value) {
		var must = value.mustLinks().contains(factory.linkType);
		var forbidden = value.forbiddenLinks().contains(factory.linkType);
		if (must) {
			return forbidden ? TruthValue.ERROR : TruthValue.TRUE;
		}
		return forbidden ? TruthValue.FALSE : TruthValue.UNKNOWN;
	}

	private Set<PartialRelation> removeFromSet(Set<PartialRelation> oldSet) {
		if (!oldSet.contains(factory.linkType)) {
			return oldSet;
		}
		var newElements = new ArrayList<PartialRelation>(oldSet);
		newElements.remove(factory.linkType);
		return Set.copyOf(newElements);
	}

	public InferredContainment mergeLink(InferredContainment oldValue, TruthValue toMerge) {
		return switch (toMerge) {
			case UNKNOWN -> oldValue;
			case TRUE -> mustLink(oldValue);
			case FALSE -> forbidLink(oldValue);
			case ERROR -> errorLink(oldValue);
		};
	}

	public InferredContainment mustLink(InferredContainment oldValue) {
		var mustLinks = oldValue.mustLinks();
		if (oldValue.contains().may() && mustLinks.isEmpty() && oldValue.forbiddenLinks().isEmpty()) {
			return concretizationInProgress() ? errorLink(oldValue) : factory.trueLink;
		}
		if (mustLinks.contains(factory.linkType)) {
			return oldValue;
		}
		if (concretizationInProgress()) {
			return errorLink(oldValue);
		}
		return new InferredContainment(oldValue.contains().meet(TruthValue.TRUE),
				addToSet(mustLinks, factory.linkType), oldValue.forbiddenLinks());
	}

	public InferredContainment forbidLink(InferredContainment oldValue) {
		var forbiddenLinks = oldValue.forbiddenLinks();
		if (oldValue.contains() == TruthValue.UNKNOWN && oldValue.mustLinks().isEmpty() && forbiddenLinks.isEmpty()) {
			return factory.falseLinkUnknownContains;
		}
		if (forbiddenLinks.contains(factory.linkType)) {
			return oldValue;
		}
		return new InferredContainment(oldValue.contains(), oldValue.mustLinks(),
				addToSet(forbiddenLinks, factory.linkType));
	}

	public InferredContainment errorLink(InferredContainment oldValue) {
		return new InferredContainment(TruthValue.ERROR, addToSet(oldValue.mustLinks(), factory.linkType),
				addToSet(oldValue.forbiddenLinks(), factory.linkType));
	}

	@Override
	public void afterInitialize(ModelSeed modelSeed) {
		RefinementUtils.refineFromSeed(this, modelSeed);
	}

	private static Set<PartialRelation> addToSet(Set<PartialRelation> oldSet, PartialRelation linkType) {
		if (oldSet.isEmpty()) {
			return Set.of(linkType);
		}
		var newElements = new ArrayList<PartialRelation>(oldSet.size() + 1);
		newElements.addAll(oldSet);
		newElements.add(linkType);
		return Set.copyOf(newElements);
	}

	public static PartialInterpretationRefiner.Factory<TruthValue, Boolean> of(
			PartialRelation linkType, Symbol<InferredContainment> symbol, ContainmentInfo info) {
		return new Factory(linkType, symbol, info);
	}

	private static class Factory implements PartialInterpretationRefiner.Factory<TruthValue, Boolean> {
		public final PartialRelation linkType;
		public final Symbol<InferredContainment> symbol;
		public final PartialRelation targetType;
		public final PartialRelation sourceType;
		public final Set<PartialRelation> supersets;
		public final Set<PartialRelation> oppositeSupersets;
		public final InferredContainment trueLink;
		public final InferredContainment falseLinkUnknownContains;

		public Factory(PartialRelation linkType, Symbol<InferredContainment> symbol, ContainmentInfo info) {
			this.linkType = linkType;
			this.symbol = symbol;
			this.sourceType = info.sourceType();
			this.targetType = info.targetType();
			this.supersets = info.supersets();
			this.oppositeSupersets = info.oppositeSupersets();
			trueLink = new InferredContainment(TruthValue.TRUE, Set.of(linkType), Set.of());
			falseLinkUnknownContains = new InferredContainment(TruthValue.UNKNOWN, Set.of(), Set.of(linkType));
		}

		@Override
		public PartialInterpretationRefiner<TruthValue, Boolean> create(
				ReasoningAdapter adapter, PartialSymbol<TruthValue, Boolean> partialSymbol) {
			return new ContainmentLinkRefiner(adapter, partialSymbol, this);
		}
	}
}
