/*
 * SPDX-FileCopyrightText: 2023-2024 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.reasoning.translator;

import tools.refinery.logic.Constraint;
import tools.refinery.logic.dnf.Query;
import tools.refinery.logic.dnf.QueryBuilder;
import tools.refinery.logic.dnf.RelationalQuery;
import tools.refinery.logic.literal.Literal;
import tools.refinery.logic.term.NodeVariable;
import tools.refinery.logic.term.Variable;
//import tools.refinery.logic.term.truthvalue.TruthValue;
import tools.refinery.logic.term.truthvalue.TruthValue;
import tools.refinery.logic.term.truthvalue.TruthValueConfidence;
import tools.refinery.store.dse.propagation.PropagationBuilder;
import tools.refinery.store.dse.transition.Rule;
import tools.refinery.store.dse.transition.RuleBuilder;
import tools.refinery.store.dse.transition.objectives.Criteria;
import tools.refinery.store.dse.transition.objectives.Criterion;
import tools.refinery.store.dse.transition.objectives.Objective;
import tools.refinery.store.dse.transition.objectives.Objectives;
import tools.refinery.store.model.ModelStoreBuilder;
import tools.refinery.store.query.view.MayConfidenceView;
import tools.refinery.store.query.view.MustConfidenceView;
import tools.refinery.store.reasoning.ReasoningAdapter;
import tools.refinery.store.reasoning.ReasoningBuilder;
import tools.refinery.store.reasoning.actions.PartialActionLiterals;
import tools.refinery.store.reasoning.interpretation.*;
import tools.refinery.store.reasoning.lifting.DnfLifter;
import tools.refinery.store.reasoning.literal.Concreteness;
import tools.refinery.store.reasoning.literal.Modality;
import tools.refinery.store.reasoning.literal.PartialLiterals;
import tools.refinery.store.reasoning.refinement.*;
import tools.refinery.store.reasoning.representation.ConfidencePartialRelation;
import tools.refinery.store.reasoning.representation.PartialRelation;
import tools.refinery.store.representation.AnySymbol;
import tools.refinery.store.representation.Symbol;

import java.util.ArrayList;
import java.util.function.BiConsumer;

import static tools.refinery.logic.literal.Literals.not;

@SuppressWarnings("UnusedReturnValue")
public final class ConfidencePartialRelationTranslator extends PartialSymbolTranslator<TruthValueConfidence, Boolean> {
	private final ConfidencePartialRelation confidencePartialRelation;
	private final PartialRelation partialRelation;
	private PartialRelationRewriter rewriter;
	private RelationalQuery query;
	private RelationalQuery may;
	private RelationalQuery must;
	private RelationalQuery candidateMay;
	private RelationalQuery candidateMust;
	private RelationalQuery candidateMayMerged;
	private RelationalQuery candidateMustMerged;
	private RoundingMode roundingMode;
	private boolean mergeCandidateWithPartial = true;
	private PartialInterpretation.Factory<TruthValue, Boolean> derivedInterpretationFactory;

	private ConfidencePartialRelationTranslator(ConfidencePartialRelation confidencePartialRelation,
												PartialRelation partialRelation) {
		super(confidencePartialRelation);
		this.confidencePartialRelation = confidencePartialRelation;
		this.partialRelation = partialRelation;
	}

	public ConfidencePartialRelation getConfidencePartialRelation() {
		return confidencePartialRelation;
	}

	public PartialRelation getPartialRelation() {
		return partialRelation;
	}

	@Override
	public ConfidencePartialRelationTranslator symbol(AnySymbol storageSymbol) {
		super.symbol(storageSymbol);
		return this;
	}

	@Override
	public <T> ConfidencePartialRelationTranslator symbol(Symbol<T> storageSymbol,
                                                          StorageRefiner.Factory<T> storageRefiner) {
		super.symbol(storageSymbol, storageRefiner);
		return this;
	}

	@Override
	public ConfidencePartialRelationTranslator interpretation(
			PartialInterpretation.Factory<TruthValueConfidence, Boolean> interpretationFactory) {
		super.interpretation(interpretationFactory);
		return this;
	}

	public ConfidencePartialRelationTranslator derivedInterpretation(PartialInterpretation.Factory<TruthValue,
			Boolean> interpretationFactory) {
		checkNotConfigured();
		if (this.derivedInterpretationFactory != null) {
			throw new IllegalStateException("Derived interpretation factory was already set");
		}
		this.derivedInterpretationFactory = interpretationFactory;
		return this;
	}

	@Override
	public ConfidencePartialRelationTranslator refiner(
			PartialInterpretationRefiner.Factory<TruthValueConfidence, Boolean> interpretationRefiner) {
		super.refiner(interpretationRefiner);
		return this;
	}

	public ConfidencePartialRelationTranslator rewriter(PartialRelationRewriter rewriter) {
		checkNotConfigured();
		if (this.rewriter != null) {
			throw new IllegalArgumentException("Rewriter was already set");
		}
		this.rewriter = rewriter;
		return this;
	}

	@Override
	public ConfidencePartialRelationTranslator initializer(PartialModelInitializer initializer) {
		super.initializer(initializer);
		return this;
	}

	@Override
	public ConfidencePartialRelationTranslator decision(Rule decisionRule) {
		super.decision(decisionRule);
		return this;
	}

	@Override
	public ConfidencePartialRelationTranslator accept(Criterion acceptanceCriterion) {
		super.accept(acceptanceCriterion);
		return this;
	}

	@Override
	public ConfidencePartialRelationTranslator exclude(Criterion exclusionCriterion) {
		super.exclude(exclusionCriterion);
		return this;
	}

	@Override
	public ConfidencePartialRelationTranslator objective(Objective objective) {
		super.objective(objective);
		return this;
	}

	public ConfidencePartialRelationTranslator query(RelationalQuery query) {
		checkNotConfigured();
		if (this.query != null) {
			throw new IllegalArgumentException("Query was already set");
		}
		this.query = query;
		return this;
	}

	public ConfidencePartialRelationTranslator may(RelationalQuery may) {
		checkNotConfigured();
		if (this.may != null) {
			throw new IllegalArgumentException("May query was already set");
		}
		this.may = may;
		return this;
	}

	public ConfidencePartialRelationTranslator mayNever() {
		var never = createQuery(confidencePartialRelation.name() + "#never", (builder, parameters) -> {
		});
		may(never);
		return this;
	}

	public ConfidencePartialRelationTranslator must(RelationalQuery must) {
		checkNotConfigured();
		if (this.must != null) {
			throw new IllegalArgumentException("Must query was already set");
		}
		this.must = must;
		return this;
	}

	public ConfidencePartialRelationTranslator candidate(RelationalQuery candidate) {
		candidateMay(candidate);
		candidateMust(candidate);
		return this;
	}

	public ConfidencePartialRelationTranslator candidateMay(RelationalQuery candidateMay) {
		checkNotConfigured();
		if (this.candidateMay != null) {
			throw new IllegalArgumentException("Candidate may query was already set");
		}
		this.candidateMay = candidateMay;
		return this;
	}

	public ConfidencePartialRelationTranslator candidateMust(RelationalQuery candidateMust) {
		checkNotConfigured();
		if (this.candidateMust != null) {
			throw new IllegalArgumentException("Candidate must query was already set");
		}
		this.candidateMust = candidateMust;
		return this;
	}

	public ConfidencePartialRelationTranslator roundingMode(RoundingMode roundingMode) {
		checkNotConfigured();
		if (this.roundingMode != null) {
			throw new IllegalArgumentException("Rounding mode was already set");
		}
		this.roundingMode = roundingMode;
		return this;
	}

	public ConfidencePartialRelationTranslator mergeCandidateWithPartial(boolean mergeCandidateWithPartial) {
		checkNotConfigured();
		this.mergeCandidateWithPartial = mergeCandidateWithPartial;
		return this;
	}

	@Override
	protected void doConfigure(ModelStoreBuilder storeBuilder) {
		setFallbackRoundingMode();
		createFallbackQueryFromRewriter();
		liftQueries(storeBuilder);
		createFallbackQueriesFromSymbol();
		setFallbackCandidateQueries();
		mergeCandidateQueries();
		createFallbackRewriter();
		createFallbackInterpretation();
		createFallbackDerivedInterpretation();
		createFallbackRefiner();
		createFallbackExclude(storeBuilder);
		createFallbackObjective();
		super.doConfigure(storeBuilder);
	}

	private void setFallbackRoundingMode() {
		if (roundingMode == null) {
			roundingMode = query == null && storageSymbol != null ? RoundingMode.PREFER_FALSE : RoundingMode.NONE;
		}
	}

	private RelationalQuery createQuery(String name, BiConsumer<QueryBuilder, NodeVariable[]> callback) {
		int arity = confidencePartialRelation.arity();
		var queryBuilder = Query.builder(name);
		var parameters = new NodeVariable[arity];
		for (int i = 0; i < arity; i++) {
			parameters[i] = queryBuilder.parameter("p" + i);
		}
		callback.accept(queryBuilder, parameters);
		return queryBuilder.build();
	}

	private RelationalQuery createQuery(String name, Constraint constraint) {
		return createQuery(name, (builder, parameters) -> builder.clause(constraint.call(parameters)));
	}

	private void createFallbackQueryFromRewriter() {
		if (rewriter != null && query == null) {
			query = createQuery(confidencePartialRelation.name(), confidencePartialRelation);
		}
	}

	private void createFallbackQueriesFromSymbol() {
		if (storageSymbol == null || storageSymbol.valueType() != TruthValueConfidence.class) {
			return;
		}
		// We checked in the guard clause that this is safe.
		@SuppressWarnings("unchecked")
		var typedStorageSymbol = (Symbol<TruthValueConfidence>) storageSymbol;
		var defaultValue = typedStorageSymbol.defaultValue();
		if (may == null && !defaultValue.may()) {
			may = createQuery(DnfLifter.decorateName(confidencePartialRelation.name(), Modality.MAY, Concreteness.PARTIAL),
					new MayConfidenceView(typedStorageSymbol));
		}
		if (must == null && !defaultValue.must()) {
			must = createQuery(DnfLifter.decorateName(confidencePartialRelation.name(), Modality.MUST, Concreteness.PARTIAL),
					new MustConfidenceView(typedStorageSymbol));
		}
	}

	private void liftQueries(ModelStoreBuilder storeBuilder) {
		if (rewriter instanceof QueryBasedRelationRewriter queryBasedRelationRewriter) {
			liftQueriesFromQueryBasedRewriter(queryBasedRelationRewriter);
		} else if (query != null) {
			liftQueriesFromFourValuedQuery(storeBuilder);
		}
	}

	private void liftQueriesFromQueryBasedRewriter(QueryBasedRelationRewriter queryBasedRelationRewriter) {
		if (may == null) {
			may = queryBasedRelationRewriter.getMay();
		}
		if (must == null) {
			must = queryBasedRelationRewriter.getMust();
		}
		if (candidateMay == null) {
			candidateMay = queryBasedRelationRewriter.getCandidateMay();
			candidateMayMerged = candidateMay;
		}
		if (candidateMust == null) {
			candidateMust = queryBasedRelationRewriter.getCandidateMust();
			candidateMustMerged = candidateMust;
		}
	}

	private void liftQueriesFromFourValuedQuery(ModelStoreBuilder storeBuilder) {
		var reasoningBuilder = storeBuilder.getAdapter(ReasoningBuilder.class);
		if (may == null) {
			may = reasoningBuilder.lift(Modality.MAY, Concreteness.PARTIAL, query);
		}
		if (must == null) {
			must = reasoningBuilder.lift(Modality.MUST, Concreteness.PARTIAL, query);
		}
		if (candidateMay == null) {
			candidateMay = reasoningBuilder.lift(Modality.MAY, Concreteness.CANDIDATE, query);
		}
		if (candidateMust == null) {
			candidateMust = reasoningBuilder.lift(Modality.MUST, Concreteness.CANDIDATE, query);
		}
	}

	private void setFallbackCandidateQueries() {
		if (candidateMay == null) {
			candidateMay = switch (roundingMode) {
				case NONE, PREFER_TRUE -> may;
				case PREFER_FALSE -> must;
			};
		}
		if (candidateMust == null) {
			candidateMust = switch (roundingMode) {
				case NONE, PREFER_FALSE -> must;
				case PREFER_TRUE -> may;
			};
		}
	}

	private void mergeCandidateQueries() {
		if (!mergeCandidateWithPartial) {
			if (candidateMayMerged == null) {
				candidateMayMerged = candidateMay;
			}
			if (candidateMustMerged == null) {
				candidateMustMerged = candidateMust;
			}
			return;
		}
		if (candidateMayMerged == null) {
			candidateMayMerged = createQuery("candidateMayMerged", (builder, arguments) -> builder
					.clause(
							candidateMay.call(arguments),
							may.call(arguments)
					));
		}
		if (candidateMustMerged == null) {
			candidateMustMerged = createQuery("candidateMustMerged", (builder, arguments) -> builder
					.clause(candidateMust.call(arguments))
					.clause(must.call(arguments)));
		}
	}

	private void createFallbackRewriter() {
		if (rewriter == null) {
			rewriter = new QueryBasedRelationRewriter(may, must, candidateMayMerged, candidateMustMerged);
		}
	}

	private void createFallbackInterpretation() {
		if (interpretationFactory == null) {
			interpretationFactory = new QueryBasedConfidenceRelationInterpretationFactory(may, must, candidateMayMerged,
					candidateMustMerged);
		}
	}

	private void createFallbackDerivedInterpretation() {
		if (derivedInterpretationFactory == null) {
			derivedInterpretationFactory = new QueryBasedRelationInterpretationFactory(may, must, candidateMayMerged,
					candidateMustMerged);
		}
	}

	private void createFallbackRefiner() {
		if (interpretationRefiner == null && storageSymbol != null && storageSymbol.valueType() == TruthValueConfidence.class) {
			// We checked in the condition that this is safe.
			@SuppressWarnings("unchecked")
			var typedStorageSymbol = (Symbol<TruthValueConfidence>) storageSymbol;
			interpretationRefiner = ConcreteRelationConfidenceRefiner.of(typedStorageSymbol, roundingMode);
		}
	}

	private void createFallbackExclude(ModelStoreBuilder storeBuilder) {
		if (excludeWasSet) {
			return;
		}
		var excludeQuery = createQuery("exclude", (builder, parameters) -> {
			var literals = new ArrayList<Literal>(parameters.length + 2);
			literals.add(PartialLiterals.must(confidencePartialRelation.call(parameters)));
			literals.add(not(PartialLiterals.may(confidencePartialRelation.call(parameters))));
			for (var parameter : parameters) {
				literals.add(PartialLiterals.must(ReasoningAdapter.EXISTS_SYMBOL.call(parameter)));
			}
			builder.clause(literals);
		});
		exclude = Criteria.whenHasMatch(excludeQuery);
		storeBuilder.tryGetAdapter(PropagationBuilder.class).ifPresent(this::configureFallbackExcludePropagator);
	}

	private void configureFallbackExcludePropagator(PropagationBuilder propagationBuilder) {
		var propagationRule = Rule.of(confidencePartialRelation.name() + "#excluded", this::configureFallbackExcludeRule);
		propagationBuilder.rule(propagationRule);
	}

	private void configureFallbackExcludeRule(RuleBuilder builder, NodeVariable p1) {
		int arity = confidencePartialRelation.arity();
		for (int i = 0; i < arity; i++) {
			var parameters = new NodeVariable[arity];
			for (int j = 0; j < arity; j++) {
				parameters[j] = i == j ? p1 : Variable.of("v" + j);
			}
			var literals = new ArrayList<Literal>(arity + 3);
			literals.add(PartialLiterals.must(confidencePartialRelation.call(parameters)));
			literals.add(not(PartialLiterals.may(confidencePartialRelation.call(parameters))));
			for (int j = 0; j < arity; j++) {
				var parameter = parameters[j];
				if (i == j) {
					literals.add(PartialLiterals.may(ReasoningAdapter.EXISTS_SYMBOL.call(parameter)));
					literals.add(not(PartialLiterals.must(ReasoningAdapter.EXISTS_SYMBOL.call(parameter))));
				} else {
					literals.add(PartialLiterals.must(ReasoningAdapter.EXISTS_SYMBOL.call(parameter)));
				}
			}
			builder.clause(literals);
		}
		builder.action(PartialActionLiterals.remove(ReasoningAdapter.EXISTS_SYMBOL, p1));
	}

	private void createFallbackObjective() {
		if (acceptWasSet && objectiveWasSet) {
			return;
		}
		var invalidCandidate = createQuery("invalidCandidate", (builder, parameters) -> builder
				.clause(
						PartialLiterals.candidateMust(confidencePartialRelation.call(parameters)),
						not(PartialLiterals.candidateMay(confidencePartialRelation.call(parameters)))
				));
		var reject = createQuery("reject", (builder, parameters) -> {
			var literals = new ArrayList<Literal>(parameters.length + 1);
			literals.add(invalidCandidate.call(parameters));
			for (var parameter : parameters) {
				literals.add(PartialLiterals.candidateMust(ReasoningAdapter.EXISTS_SYMBOL.call(parameter)));
			}
			builder.clause(literals);
		});
		if (!acceptWasSet) {
			accept = Criteria.whenNoMatch(reject);
		}
		if (!objectiveWasSet) {
			objective = Objectives.count(reject);
		}
	}

	public PartialRelationRewriter getRewriter() {
		checkConfigured();
		return rewriter;
	}

	public static ConfidencePartialRelationTranslator of(ConfidencePartialRelation confidencePartialRelation,
														 PartialRelation partialRelation) {
		return new ConfidencePartialRelationTranslator(confidencePartialRelation, partialRelation);
	}

	public PartialInterpretation.Factory<TruthValue, Boolean> getDerivedInterpretationFactory() {
		checkConfigured();
		return derivedInterpretationFactory;
	}
}
