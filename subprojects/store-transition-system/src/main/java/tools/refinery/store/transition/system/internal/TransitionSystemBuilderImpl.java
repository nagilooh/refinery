package tools.refinery.store.transition.system.internal;

import tools.refinery.logic.dnf.Dnf;
import tools.refinery.logic.dnf.Query;
import tools.refinery.logic.literal.CallLiteral;
import tools.refinery.logic.literal.CallPolarity;
import tools.refinery.logic.literal.ConstantLiteral;
import tools.refinery.logic.literal.Literal;
import tools.refinery.logic.term.NodeVariable;
import tools.refinery.logic.term.Variable;
import tools.refinery.logic.term.truthvalue.TruthValue;
import tools.refinery.store.adapter.AbstractModelAdapterBuilder;
import tools.refinery.store.dse.propagation.PropagationBuilder;
import tools.refinery.store.dse.transition.ExclusionPropagator;
import tools.refinery.store.dse.transition.Rule;
import tools.refinery.store.dse.transition.actions.ActionLiteral;
import tools.refinery.store.dse.transition.objectives.Criterion;
import tools.refinery.store.model.ModelStore;
import tools.refinery.store.model.ModelStoreBuilder;
import tools.refinery.store.query.ModelQueryBuilder;
import tools.refinery.store.reasoning.ReasoningBuilder;
import tools.refinery.store.reasoning.representation.AnyPartialSymbol;
import tools.refinery.store.reasoning.representation.PartialRelation;
import tools.refinery.store.reasoning.translator.AnyPartialSymbolTranslator;
import tools.refinery.store.reasoning.translator.PartialRelationTranslator;
import tools.refinery.store.reasoning.translator.predicate.PredicateTranslator;
import tools.refinery.store.representation.AnySymbol;
import tools.refinery.store.representation.Symbol;
import tools.refinery.store.transition.system.TransitionSystemBuilder;
import tools.refinery.store.transition.system.statespace.Transition;
import tools.refinery.store.transition.system.statespace.TransitionRule;

import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import static tools.refinery.logic.literal.Literals.not;
import static tools.refinery.store.reasoning.actions.PartialActionLiterals.add;
import static tools.refinery.store.reasoning.actions.PartialActionLiterals.remove;
import static tools.refinery.store.reasoning.literal.PartialLiterals.may;
import static tools.refinery.store.reasoning.literal.PartialLiterals.must;

public class TransitionSystemBuilderImpl extends AbstractModelAdapterBuilder<TransitionSystemStoreAdapterImpl> implements TransitionSystemBuilder {

	private final LinkedHashSet<Transition.Builder> transitions = new LinkedHashSet<>();
	private final LinkedHashSet<Criterion> accepts = new LinkedHashSet<>();

	@Override
	public TransitionSystemBuilder transition(TransitionRule rule) {
		transitions.add(Transition.Builder.of(rule));
		return this;
	}

	@Override
	public TransitionSystemBuilder accept(Criterion criterion) {
		accepts.add(criterion);
		return this;
	}

	@Override
	protected void doConfigure(ModelStoreBuilder storeBuilder) {
		var queryEngine = storeBuilder.getAdapter(ModelQueryBuilder.class);
		transitions.forEach(t -> {
			List<PartialRelation> parameterTypes = new ArrayList<>();
			for (var i = 0; i < t.precondition.arity(); ++i) {
				parameterTypes.add(null);
			}
			var translator = new PredicateTranslator(
					t.preconditionRelation,
					t.precondition,
					parameterTypes,
					Set.of(),
					true,
					TruthValue.UNKNOWN
			);
			storeBuilder.with(translator);
			queryEngine.queries(t.mayPrecondition);
			for (var literal : t.action.getActionLiterals()) {
				queryEngine.queries(literal.getQueries());
			}
		});
		accepts.forEach(x -> x.configure(storeBuilder));

		var partialSymbolTranslators = storeBuilder.getAdapter(ReasoningBuilder.class).getPartialSymbolTranslators();
		storeBuilder.tryGetAdapter(PropagationBuilder.class).ifPresent(propagationBuilder -> {
			propagationBuilder.propagator(new ExclusionPropagator());

//			List<Rule> propagationRules = new ArrayList<>();
//			collectBasicDerivedRelationPropagationRules(partialSymbolTranslators, propagationRules);
//			for (var propagationRule : propagationRules) {
//				propagationBuilder.rule(propagationRule);
//			}
		});

		// build cone-of-influence for base relations
		var coneOfInfluence = buildConeOfInfluence(partialSymbolTranslators);
		transitions.forEach(t -> t.setConeOfInfluence(coneOfInfluence));

		super.doConfigure(storeBuilder);
	}

	@Override
	protected TransitionSystemStoreAdapterImpl doBuild(ModelStore store) {
		List<Transition.Builder> transitionsList = List.copyOf(transitions);
		List<Criterion> acceptsList = List.copyOf(accepts);

		return new TransitionSystemStoreAdapterImpl(store, transitionsList, acceptsList);
	}

	private Map<AnySymbol, Set<SimpleEntry<Symbol<?>, Map<Integer, Integer>>>> buildConeOfInfluence(
			Map<AnyPartialSymbol, AnyPartialSymbolTranslator> partialSymbolTranslators) {
		Map<PartialRelation, Symbol<?>> baseRelationSymbols = new LinkedHashMap<>();
		Map<PartialRelation, Set<SimpleEntry<PartialRelation, Map<Integer, Integer>>>> influences = new LinkedHashMap<>();

		for (var translator : partialSymbolTranslators.values()) {
			if (translator instanceof PartialRelationTranslator partialRelationTranslator) {
				var partialRelation = partialRelationTranslator.getPartialRelation();
				var query = partialRelationTranslator.getQuery();
				var symbol = (Symbol<?>) partialRelationTranslator.getStorageSymbol();
				if (symbol != null) {
					baseRelationSymbols.put(partialRelation, symbol);
				}
				if (query != null) {
					var dnf = query.getDnf();
					var parameters = new LinkedHashMap<Integer, NodeVariable>();
					for (int i = 0; i < query.arity(); ++i) {
						parameters.put(i, dnf.getSymbolicParameters().get(i).getVariable().asNodeVariable());
					}
					collectInfluences(partialRelation, dnf, influences, parameters);
				}
			}
		}

		Map<AnySymbol, Set<SimpleEntry<Symbol<?>, Map<Integer, Integer>>>> result = new LinkedHashMap<>();
		for (var entry : influences.entrySet()) {
			var baseRelation = entry.getKey();
			var baseRelationSymbol = baseRelationSymbols.get(baseRelation);
			if (baseRelationSymbol != null) {
				var influencedRelations = entry.getValue();
				var influencedWithEmptyMapping =
						influencedRelations.stream().filter(e -> e.getValue().isEmpty()).map(SimpleEntry::getKey).collect(Collectors.toSet());
				Set<SimpleEntry<Symbol<?>, Map<Integer, Integer>>> influencedSymbols = new LinkedHashSet<>();
				for (var influencedRelation : influencedRelations) {
					if (!influencedWithEmptyMapping.contains(influencedRelation.getKey()) || influencedRelation.getValue().isEmpty()) {
						var symbol = baseRelationSymbols.get(influencedRelation.getKey());
						if (symbol != null) {
							influencedSymbols.add(new SimpleEntry<>(symbol, influencedRelation.getValue()));
						}
					}
				}
				result.put(baseRelationSymbol, influencedSymbols);
			}
		}
		return result;
	}

	private void collectInfluences(PartialRelation derivedRelation, Dnf dnf,
								   Map<PartialRelation, Set<SimpleEntry<PartialRelation, Map<Integer, Integer>>>> influences,
								   Map<Integer, NodeVariable> parameters) {
		for (var clause : dnf.getClauses()) {
			for (var literal : clause.literals()) {
				if (literal instanceof CallLiteral callLiteral) {
					var target = callLiteral.getTarget();
					var arguments = callLiteral.getArguments();
					if (target instanceof PartialRelation partialRelation) {
						Map<Integer, Integer> finalParameterMapping = new LinkedHashMap<>();
						for (var entry : parameters.entrySet()) {
							int originalParameterIndex = entry.getKey();
							NodeVariable argument = entry.getValue();
							int baseRelationParameterIndex = arguments.indexOf(argument);
							if (baseRelationParameterIndex != -1) {
								finalParameterMapping.put(originalParameterIndex, baseRelationParameterIndex);
							}
						}
						var influenced = new SimpleEntry<>(derivedRelation, finalParameterMapping);
						influences.computeIfAbsent(partialRelation, x -> new LinkedHashSet<>()).add(influenced);
					} else if (target instanceof Dnf dnfTarget) {
						Map<Integer, NodeVariable> updatedParameters = new LinkedHashMap<>();
						for (var entry : parameters.entrySet()) {
							int originalParameterIndex = entry.getKey();
							NodeVariable argument = entry.getValue();
							int dnfParameterIndex = arguments.indexOf(argument);
							if (dnfParameterIndex != -1) {
								var newVar =
										dnfTarget.getSymbolicParameters().get(dnfParameterIndex).getVariable().asNodeVariable();
								updatedParameters.put(originalParameterIndex, newVar);
							}
						}
						collectInfluences(derivedRelation, dnfTarget, influences, updatedParameters);
					}
				}
			}
		}
	}

	private void collectBasicDerivedRelationPropagationRules(Map<AnyPartialSymbol, AnyPartialSymbolTranslator> partialSymbolTranslators,
															 List<Rule> propagationRules) {
		for (var translator : partialSymbolTranslators.values()) {
			if (translator instanceof PartialRelationTranslator partialRelationTranslator) {
				var relation = partialRelationTranslator.getPartialRelation();
				var relationSymbol = (Symbol<?>) partialRelationTranslator.getStorageSymbol();
				var query = partialRelationTranslator.getQuery();
				if (query != null && relationSymbol != null && relationSymbol.valueType() == TruthValue.class) {
					var dnf = query.getDnf();

					// Propagation rule for merging the literals of the single clause true if the relation is true
					if (dnf.getClauses().size() == 1) {
						var clause = dnf.getClauses().getFirst();

						var success = true;
						int[][] argumentMappings = new int[clause.literals().size()][];
						List<Literal> literals = clause.literals();
						for (int i = 0; i < literals.size(); i++) {
							var literal = literals.get(i);
							if (Objects.requireNonNull(literal) instanceof CallLiteral callLiteral) {
								argumentMappings[i] = new int[callLiteral.getArguments().size()];
								success = createArgumentMapping(dnf, callLiteral, argumentMappings[i]);
								if (!success) {
									break;
								}
							} else if (literal instanceof ConstantLiteral constantLiteral) {
								argumentMappings[i] = new int[1];
								success = createArgumentMapping(dnf, constantLiteral, argumentMappings[i]);
								if (!success) {
									break;
								}
							} else {
								success = false;
								break;
							}
						}

						if (success) {
							NodeVariable[] parameters = new NodeVariable[dnf.arity()];
							for (int i = 0; i < dnf.arity(); ++i) {
								parameters[i] = NodeVariable.of("p" + i);
							}

							List<CalledRelation> calledRelations = new ArrayList<>(clause.literals().size());
							for (int i = 0; i < clause.literals().size(); ++i) {
								var literal = literals.get(i);
								if (Objects.requireNonNull(literal) instanceof CallLiteral callLiteral && callLiteral.getTarget() instanceof PartialRelation calledRelation) {
									var arguments = callLiteral.getArguments();
									NodeVariable[] actionParams = new NodeVariable[arguments.size()];
									for (int j = 0; j < arguments.size(); ++j) {
										actionParams[j] = parameters[argumentMappings[i][j]];
									}
									calledRelations.add(new CalledRelation(calledRelation, callLiteral.getPolarity(),
											actionParams));
								} else if (literal instanceof ConstantLiteral) {
									// no need to add an action literal for a constant literal
								} else {
									success = false;
									break;
								}
							}
							if (!success) {
								return;
							}

							List<NodeVariable> literalParameters = new ArrayList<>();
							var calledQuery = Query.of(builder -> {
								Map<NodeVariable, NodeVariable> calledParameters = new LinkedHashMap<>();

								List<Literal> ls = new ArrayList<>(calledRelations.size());
								for (CalledRelation calledRelation : calledRelations) {
									var calledParams = new NodeVariable[calledRelation.arguments().length];
									for (int i = 0; i < calledRelation.arguments().length; ++i) {
										var calledRelationArgument = calledRelation.arguments()[i];
										calledParams[i] = calledParameters.computeIfAbsent(calledRelationArgument, a -> NodeVariable.of(a.getName()));
									}
									ls.add(calledRelation.relation()
											.call(calledRelation.polarity(), calledParams));
								}

								List<NodeVariable> params = new ArrayList<>();
								for (var entry : calledParameters.entrySet()) {
									literalParameters.add(entry.getKey());
									params.add(entry.getValue());
								}
								builder.parameters(params);
								builder.clause(ls);
							});

							propagationRules.add(Rule.of(
									partialRelationTranslator.getPartialRelation().name() + "_helper" + propagationRules.size(),
									builder -> builder
											.parameters(literalParameters)
											.clause(must(relation.call(parameters)),
													not(must(calledQuery.call(literalParameters.toArray(new NodeVariable[0])))))
											.action(calledRelations.stream().map(cr -> switch (cr.polarity()) {
												case POSITIVE -> add(cr.relation(), cr.arguments());
												case NEGATIVE -> remove(cr.relation(), cr.arguments());
												default ->
														throw new IllegalStateException("Unexpected CallPolarity: " + cr.polarity());
											}).toArray(ActionLiteral[]::new))
							));
						}
					}

					// Propagation rules for merging the clauses false if the relation is false
					for (var clause : dnf.getClauses()) {
						if (clause.literals().size() == 1) {
							var literal = clause.literals().getFirst();
							if (literal instanceof CallLiteral callLiteral && callLiteral.getTarget() instanceof PartialRelation calledRelation) {
								var arguments = callLiteral.getArguments();
								int[] argumentMapping = new int[arguments.size()];
								boolean success = createArgumentMapping(dnf, callLiteral, argumentMapping);
								if (!success) {
									continue;
								}

								NodeVariable[] parameters = new NodeVariable[dnf.arity()];
								for (int i = 0; i < dnf.arity(); ++i) {
									parameters[i] = NodeVariable.of("p" + i);
								}
								NodeVariable[] actionParams = new NodeVariable[arguments.size()];
								for (int i = 0; i < arguments.size(); ++i) {
									actionParams[i] = parameters[argumentMapping[i]];
								}

								var relationQuery = Query.of(builder -> {
									var relationParams = new NodeVariable[parameters.length];
									for (int i = 0; i < parameters.length; ++i) {
										relationParams[i] = NodeVariable.of(parameters[i].getName());
									}
									builder
											.parameters(relationParams)
											.clause(relation.call(relationParams));
								});

								propagationRules.add(Rule.of(
										partialRelationTranslator.getPartialRelation().name() + "_helper" + propagationRules.size(),
										builder -> builder
												.parameters(actionParams)
												.clause(not(may(relationQuery.call(parameters))),
														may(calledRelation.call(actionParams)))
												.action(remove(calledRelation, actionParams))
								));
							}
						}
					}
				}
			}
		}
	}

	private boolean createArgumentMapping(Dnf dnf, Literal literal, int[] argumentMapping) {
		boolean success = true;
		switch (literal) {
		case CallLiteral callLiteral -> {
			List<Variable> callLiteralArguments = callLiteral.getArguments();
			for (int i = 0; i < callLiteralArguments.size(); i++) {
				success = false;
				for (int j = 0; j < dnf.getSymbolicParameters().size(); ++j) {
					if (callLiteralArguments.get(i) == dnf.getSymbolicParameters().get(j).getVariable()) {
						argumentMapping[i] = j;
						success = true;
						break;
					}
				}
				if (!success) {
					break;
				}
			}
		}
		case ConstantLiteral constantLiteral -> {
			success = false;
			for (int j = 0; j < dnf.getSymbolicParameters().size(); ++j) {
				if (constantLiteral.getVariable() == dnf.getSymbolicParameters().get(j).getVariable()) {
					argumentMapping[0] = j;
					success = true;
					break;
				}
			}
		}
		default -> success = false;
		}
		return success;
	}

	private record CalledRelation(PartialRelation relation, CallPolarity polarity, NodeVariable[] arguments) {
	}
}
