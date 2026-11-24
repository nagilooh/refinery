package tools.refinery.measurement;

import tools.refinery.logic.dnf.Query;
import tools.refinery.logic.dnf.RelationalQuery;
import tools.refinery.logic.literal.Literals;
import tools.refinery.logic.term.Variable;
import tools.refinery.logic.term.cardinalityinterval.CardinalityIntervals;
import tools.refinery.logic.term.int_.IntTerms;
import tools.refinery.logic.term.truthvalue.TruthValue;
import tools.refinery.store.dse.modification.ModificationAdapter;
import tools.refinery.store.dse.propagation.PropagationAdapter;
import tools.refinery.store.dse.transition.DesignSpaceExplorationAdapter;
import tools.refinery.store.dse.transition.Rule;
import tools.refinery.store.query.interpreter.QueryInterpreterAdapter;
import tools.refinery.store.reasoning.ReasoningAdapter;
import tools.refinery.store.reasoning.ReasoningBuilder;
import tools.refinery.store.reasoning.actions.CleanupActionLiteral;
import tools.refinery.store.reasoning.actions.CreateNewActionLiteral;
import tools.refinery.store.reasoning.actions.PartialActionLiterals;
import tools.refinery.store.reasoning.interpretation.PartialNeighborhoodCalculator;
import tools.refinery.store.reasoning.literal.Concreteness;
import tools.refinery.store.reasoning.literal.Modality;
import tools.refinery.store.reasoning.representation.PartialRelation;
import tools.refinery.store.reasoning.seed.ModelSeed;
import tools.refinery.store.reasoning.translator.ConcretizationSettings;
import tools.refinery.store.reasoning.translator.containment.ContainmentHierarchyTranslator;
import tools.refinery.store.reasoning.translator.metamodel.Metamodel;
import tools.refinery.store.reasoning.translator.metamodel.MetamodelBuilder;
import tools.refinery.store.reasoning.translator.metamodel.MetamodelTranslator;
import tools.refinery.store.reasoning.translator.multiobject.MultiObjectTranslator;
import tools.refinery.store.reasoning.translator.predicate.BasePredicateTranslator;
import tools.refinery.store.reasoning.translator.predicate.PredicateTranslator;
import tools.refinery.store.statecoding.StateCoderAdapter;
import tools.refinery.store.transition.system.TransitionSystemAdapter;
import tools.refinery.store.transition.system.statespace.TransitionRule;
import tools.refinery.store.tuple.Tuple;

import java.io.File;
import java.util.*;

import static tools.refinery.logic.literal.Literals.not;
import static tools.refinery.logic.term.truthvalue.TruthValue.*;

public class StatechartMeasurement extends MeasurementBase {

	String fileNamePrefix = "statechart-";

	Map<Tuple, TruthValue> stateClassSet = new HashMap<>();
	Map<Tuple, TruthValue> transitionClassSet = new HashMap<>();

	Map<Tuple, TruthValue> outgoingTransitionSet = new HashMap<>();
	Map<Tuple, TruthValue> incomingTransitionSet = new HashMap<>();
	Map<Tuple, TruthValue> sourceSet = new HashMap<>();
	Map<Tuple, TruthValue> targetSet = new HashMap<>();

	Map<Tuple, TruthValue> activeSet = new HashMap<>();


	Map<String, Map<Tuple, TruthValue>> classMaps = new HashMap<>();
	Map<String, Map<Tuple, TruthValue>> relationMaps = new HashMap<>();
	Map<String, Map<Tuple, TruthValue>> attributeMaps = new HashMap<>();


	static final String StateClassName = "State";
	static final String TransitionClassName = "Transition";

	static final String outgoingTransitionRelationName = "outgoing";
	static final String incomingTransitionRelationName = "incoming";
	static final String sourceRelationName = "source";
	static final String targetRelationName = "target";

	static final String activeAttributeName = "active";


    PartialRelation stateClass = new PartialRelation(StateClassName, 1);
	PartialRelation transitionClass = new PartialRelation(TransitionClassName, 1);
    PartialRelation outgoingTransition = new PartialRelation(outgoingTransitionRelationName, 2);
	PartialRelation incomingTransition = new PartialRelation(incomingTransitionRelationName, 2);
    PartialRelation source = new PartialRelation(sourceRelationName, 2);
    PartialRelation target = new PartialRelation(targetRelationName, 2);
	PartialRelation targetInvalidMultiplicity = new PartialRelation("target::invalidMultiplicity", 1);

	PartialRelation active = new PartialRelation(activeAttributeName, 1);


	MetamodelBuilder builder = Metamodel.builder()
			.type(stateClass)
			.type(transitionClass)
			.reference(outgoingTransition, referenceBuilder -> referenceBuilder
					.source(stateClass)
					.target(transitionClass)
					.defaultValue(TruthValue.FALSE))
			.reference(incomingTransition, referenceBuilder -> referenceBuilder
					.source(stateClass)
					.target(transitionClass)
					.defaultValue(TruthValue.FALSE))
			;

	Metamodel metamodel = builder.build();

	RelationalQuery hasLoop;
	RelationalQuery hasLoopLifted;
	RelationalQuery noOutgoing;
	RelationalQuery noOutgoingLifted;


    PartialRelation stateTransitionPreconditionPartialRelation = new PartialRelation("stateTransition_precondition", 3);
    TransitionRule stateTransition = new TransitionRule(stateTransitionPreconditionPartialRelation,
            Rule.of("stateTransition", (builder, t, from, to) -> builder
            .clause(
                    transitionClass.call(t),
					stateClass.call(from),
					stateClass.call(to),
                    outgoingTransition.call(from, t),
                    incomingTransition.call(to, t),
					active.call(from)
            )
            .action(
					PartialActionLiterals.modify(active, FALSE, from),
					PartialActionLiterals.modify(active, TRUE, to)
            )));

	PartialRelation injectLoopPreconditionPartialRelation = new PartialRelation("injectLoop_precondition", 1);
	TransitionRule injectLoop = new TransitionRule(injectLoopPreconditionPartialRelation,
			Rule.of("injectLoop", (builder, s) -> builder
					.clause(
							stateClass.call(s)
					)
					.action(t -> List.of(
							new CreateNewActionLiteral(t),
							PartialActionLiterals.modify(transitionClass, TRUE, t),
							PartialActionLiterals.modify(outgoingTransition, TRUE, s, t),
							PartialActionLiterals.modify(incomingTransition, TRUE, s, t)
					)
			)));

	PartialRelation repairLoopPreconditionPartialRelation = new PartialRelation("repairLoop_precondition", 2);
	TransitionRule repairLoop = new TransitionRule(repairLoopPreconditionPartialRelation,
			Rule.of("repairLoop", (builder, s, t) -> builder
					.clause(
							stateClass.call(s),
							transitionClass.call(t),
							outgoingTransition.call(s, t),
							incomingTransition.call(s, t)
					)
					.action(
							new CleanupActionLiteral(t, true),
							PartialActionLiterals.modify(stateClass, TRUE, s)
					)));

	PartialRelation injectNoOutgoingPreconditionPartialRelation = new PartialRelation(
			"injectNoOutgoing_precondition", 2);
	TransitionRule injectNoOutgoing = new TransitionRule(injectNoOutgoingPreconditionPartialRelation,
			Rule.of("injectNoOutgoing", (builder, s, t) -> builder
					.clause(
							stateClass.call(s),
							transitionClass.call(t),
							outgoingTransition.call(s, t)
					)
					.action(

							new CleanupActionLiteral(t, true)
					)));

	PartialRelation repairNoOutgoingPreconditionPartialRelation = new PartialRelation(
			"repairNoOutgoing_precondition", 2);
	TransitionRule repairNoOutgoing = new TransitionRule(repairNoOutgoingPreconditionPartialRelation,
			Rule.of("repairNoOutgoing", (builder, s1, s2) -> builder
					.clause(
							stateClass.call(s1),
							stateClass.call(s2),
							not(outgoingTransition.call(s1, Variable.of())),
							not(outgoingTransition.call(s2, Variable.of())),
							s1.notEquivalent(s2)
					)
					.action((t1, t2) -> List.of(
							new CreateNewActionLiteral(t1),
							PartialActionLiterals.modify(transitionClass, TRUE, t1),
							PartialActionLiterals.modify(outgoingTransition, TRUE, s1, t1),
							PartialActionLiterals.modify(incomingTransition, TRUE, s2, t1),
							new CreateNewActionLiteral(t2),
							PartialActionLiterals.modify(transitionClass, TRUE, t2),
							PartialActionLiterals.modify(outgoingTransition, TRUE, s2, t2),
							PartialActionLiterals.modify(incomingTransition, TRUE, s1, t2)
					))));



    ModelSeed.Builder modelSeedBuilder;

	public void init(int size, MeasurementScenario scenario, Abstraction abstraction, boolean visualize) {
		this.visualize = visualize;
		this.scenario = scenario;
		this.abstraction = abstraction;
		this.size = size;

		if (visualize) {
			visualizationOutputPath = "output_visualization/statechart_" + size;
		}

		var filenameScenario = scenario.toString().toLowerCase();
		filenameScenario = switch (filenameScenario) {
			case "query" -> "repair";
			case "simulate" -> "batch";
			default -> filenameScenario;
		};
		var filenameAbstraction = abstraction.toString().toLowerCase();
		filenameAbstraction = switch (filenameAbstraction) {
			case "concrete" -> "";
			case "abstract" -> "-abstract";
			default -> filenameScenario;
		};
		fileNamePrefix = fileNamePrefix + filenameScenario + filenameAbstraction + "-" + size + "-";
		System.out.println(fileNamePrefix);

		hasLoop = Query.of("hasLoop", (builder, s, t) -> builder
				.clause(
						stateClass.call(s),
						transitionClass.call(t),
						outgoingTransition.call(s, t),
						incomingTransition.call(s, t)
				));

		noOutgoing = Query.of("noOutgoing", (builder, s) -> builder
				.clause(
						stateClass.call(s),
						not(outgoingTransition.call(s, Variable.of()))
				));

		classMaps.put(StateClassName, stateClassSet);
		classMaps.put(TransitionClassName, transitionClassSet);
		relationMaps.put(outgoingTransitionRelationName, outgoingTransitionSet);
		relationMaps.put(incomingTransitionRelationName, incomingTransitionSet);
		relationMaps.put(sourceRelationName, sourceSet);
		relationMaps.put(targetRelationName, targetSet);
		attributeMaps.put(activeAttributeName, activeSet);

		storeBuilder = storeBuilder
				.with(QueryInterpreterAdapter.builder())
				.with(PropagationAdapter.builder())
				.with(StateCoderAdapter.builder()
						.stateCodeCalculatorFactory(PartialNeighborhoodCalculator.factory(Concreteness.PARTIAL, 7))
				)
				.with(ModificationAdapter.builder())
				.with(DesignSpaceExplorationAdapter.builder())
				.with(ReasoningAdapter.builder())
				.with(new MultiObjectTranslator())
				.with(new MetamodelTranslator(metamodel))
				.with(new BasePredicateTranslator(active, List.of(stateClass), Set.of(), UNKNOWN,
						new ConcretizationSettings(true, true)));

		var reasoningBuilder = storeBuilder.getAdapter(ReasoningBuilder.class);

		hasLoopLifted = reasoningBuilder.lift(Modality.MUST, Concreteness.PARTIAL, hasLoop);
		noOutgoingLifted = reasoningBuilder.lift(Modality.MUST, Concreteness.PARTIAL, noOutgoing);

		queries.add(hasLoopLifted);
		queries.add(noOutgoingLifted);

		switch (scenario) {
		case QUERY -> {
			// no additional transitions or translators needed
			storeBuilder = storeBuilder
					.with(TransitionSystemAdapter.builder());
		}
		case SIMULATE -> {
			storeBuilder = storeBuilder
					.with(TransitionSystemAdapter.builder()
							.transition(stateTransition)
							.accept(_ -> () -> false) // full state space exploration
					)
					.with(new PredicateTranslator(
							stateTransitionPreconditionPartialRelation,
							stateTransition.rule().getPrecondition(),
							List.of(transitionClass, stateClass, stateClass), Set.of(), true, UNKNOWN));
			fireOneTransitionRules.add(stateTransition.rule().getName());
		}
		case INJECT -> {
			storeBuilder = storeBuilder
					.with(TransitionSystemAdapter.builder()
							.transition(injectLoop)
							.transition(injectNoOutgoing))
					.with(new PredicateTranslator(
							injectLoopPreconditionPartialRelation,
							injectLoop.rule().getPrecondition(),
							List.of(stateClass), Set.of(), true, UNKNOWN))
					.with(new PredicateTranslator(
							injectNoOutgoingPreconditionPartialRelation,
							injectNoOutgoing.rule().getPrecondition(),
							List.of(stateClass, transitionClass), Set.of(), true, UNKNOWN))
			;
			fireOneTransitionRules.add(injectLoop.rule().getName());
			fireAllTransitionRules.add(injectNoOutgoing.rule().getName());
		}
		case REPAIR -> {
			storeBuilder = storeBuilder
					.with(TransitionSystemAdapter.builder()
							.transition(repairLoop)
							.transition(repairNoOutgoing)
					)
					.with(new PredicateTranslator(
							repairLoopPreconditionPartialRelation,
							repairLoop.rule().getPrecondition(),
							List.of(stateClass, transitionClass), Set.of(), true, UNKNOWN))
					.with(new PredicateTranslator(
							repairNoOutgoingPreconditionPartialRelation,
							repairNoOutgoing.rule().getPrecondition(),
							List.of(stateClass, stateClass), Set.of(), true, UNKNOWN))
			;
			fireOneTransitionRules.add(repairLoop.rule().getName());
			fireOneTransitionRules.add(repairNoOutgoing.rule().getName());
		}
		default -> throw new IllegalArgumentException("Invalid scenario for this measurement: " + scenario);
		}

		String pathToCsvFolder = "input/models/statechart_fast";

		for (var csvFile :
				Objects.requireNonNull(new File(pathToCsvFolder).listFiles((dir, name) -> name.startsWith(fileNamePrefix) &&
						name.endsWith(".csv") && classMaps.keySet().stream().anyMatch(name::contains)))) {
			String relationName = csvFile.getName().replace(fileNamePrefix, "").replace(".csv", "");
//			System.out.println("Loading class: " + relationName);
			var inputMap = classMaps.get(relationName);
			System.out.println("Loading class: " + relationName + " from file: " + csvFile.getName());
			try (java.io.BufferedReader br = new java.io.BufferedReader(new java.io.FileReader(csvFile))) {
				String line;
				while ((line = br.readLine()) != null) {
					String[] values = line.split(",");
					inputMap.put(Tuple.of(Integer.parseInt(values[0])), TruthValue.valueOf(values[1].toUpperCase()));
				}
			} catch (java.io.IOException e) {
				e.printStackTrace();
			}
		}
		for (var csvFile :
				Objects.requireNonNull(new File(pathToCsvFolder).listFiles((dir, name) -> name.startsWith(fileNamePrefix) &&
						name.endsWith(".csv") && relationMaps.keySet().stream().anyMatch(name::contains)))) {
			String relationName = csvFile.getName().replace(fileNamePrefix, "").replace(".csv", "");
			var inputMap = relationMaps.get(relationName);
			try (java.io.BufferedReader br = new java.io.BufferedReader(new java.io.FileReader(csvFile))) {
				String line;
				while ((line = br.readLine()) != null) {
					String[] values = line.split(",");
					inputMap.put(Tuple.of(Integer.parseInt(values[0]), Integer.parseInt(values[1])),
							TruthValue.valueOf(values[2].toUpperCase()));
				}
			} catch (java.io.IOException e) {
				e.printStackTrace();
			}
		}
		activeSet.put(Tuple.of(0), TRUE); // set the initial state as active

		var nodeCount = classMaps.values().stream().flatMap(map -> map.keySet().stream())
				.mapToInt(t -> t.get(0)).max().orElse(0) + 1;
		System.out.println(nodeCount);
		modelSeedBuilder = ModelSeed.builder(nodeCount)
				.seed(MultiObjectTranslator.COUNT_SYMBOL, builder -> builder
						.reducedValue(CardinalityIntervals.ONE)
				)
				.seed(ContainmentHierarchyTranslator.CONTAINED_SYMBOL, builder ->
						builder.reducedValue(UNKNOWN))
				.seed(ContainmentHierarchyTranslator.CONTAINER_SYMBOL, builder ->
						builder.reducedValue(UNKNOWN))
				.seed(ContainmentHierarchyTranslator.CONTAINS_SYMBOL, builder ->
						builder.reducedValue(UNKNOWN))
		;


		for (var entry : classMaps.entrySet()) {
			String relationName = entry.getKey();
			Map<Tuple, TruthValue> valueMap = entry.getValue();
			var partialRelation = switch (relationName) {
				case StateClassName -> stateClass;
				case TransitionClassName -> transitionClass;
				default -> throw new IllegalArgumentException("Unknown relation name: " + relationName);
			};
			modelSeedBuilder.seed(partialRelation, builder -> {
				for (var valueEntry : valueMap.entrySet()) {
					builder.put(valueEntry.getKey(), valueEntry.getValue());
				}
				builder.reducedValue(UNKNOWN);
			});
		}


		for (var mapEntry : relationMaps.entrySet()) {
			String relationName = mapEntry.getKey();
			Map<Tuple, TruthValue> valueMap = mapEntry.getValue();
			var partialRelation = switch (relationName) {
				case sourceRelationName -> source;
				case targetRelationName -> target;
				default -> null;
			};
			if (partialRelation == null) {
				continue;
			}
			modelSeedBuilder.seed(partialRelation, builder -> {
				for (var valueEntry : valueMap.entrySet()) {
					builder.put(valueEntry.getKey(), valueEntry.getValue());
				}
				builder.reducedValue(UNKNOWN);
			});
		}


		for (var mapEntry : relationMaps.entrySet()) {
			String relationName = mapEntry.getKey();
			Map<Tuple, TruthValue> valueMap = mapEntry.getValue();
			var partialRelation = switch (relationName) {
				case outgoingTransitionRelationName -> outgoingTransition;
				case incomingTransitionRelationName -> incomingTransition;
				default -> null;
			};
			if (partialRelation == null) {
				continue;
			}
			modelSeedBuilder.seed(partialRelation, builder -> {
				for (var valueEntry : valueMap.entrySet()) {
					builder.put(valueEntry.getKey(), valueEntry.getValue());
				}
				builder.reducedValue(FALSE);
			});
		}


		for (var entry : attributeMaps.entrySet()) {
			String relationName = entry.getKey();
			Map<Tuple, TruthValue> valueMap = entry.getValue();
			var partialRelation = switch (relationName) {
				case activeAttributeName -> active;
				default -> throw new IllegalArgumentException("Unknown relation name: " + relationName);
			};
			modelSeedBuilder.seed(partialRelation, builder -> {
				for (var valueEntry : valueMap.entrySet()) {
					builder.put(valueEntry.getKey(), valueEntry.getValue());
				}
				builder.reducedValue(FALSE);
			});
		}


		switch (scenario) {
		case SIMULATE -> {
			modelSeedBuilder.seed(stateTransitionPreconditionPartialRelation, builder -> builder.reducedValue(UNKNOWN));
		}
		case INJECT -> {
			modelSeedBuilder.seed(injectLoopPreconditionPartialRelation, builder -> builder.reducedValue(UNKNOWN));
			modelSeedBuilder.seed(injectNoOutgoingPreconditionPartialRelation, builder -> builder.reducedValue(UNKNOWN));
		}
		case REPAIR -> {
			modelSeedBuilder.seed(repairLoopPreconditionPartialRelation, builder -> builder.reducedValue(UNKNOWN));
			modelSeedBuilder.seed(repairNoOutgoingPreconditionPartialRelation, builder -> builder.reducedValue(UNKNOWN));
		}
		}

		this.modelSeed = this.modelSeedBuilder.build();
		super.init();
    }

	@Override
	protected boolean renderTransitionsToAlreadyVisitedStates() {
		return true;
	}
}

