package tools.refinery.measurement;

import tools.refinery.logic.term.cardinalityinterval.CardinalityIntervals;
import tools.refinery.logic.term.truthvalue.TruthValue;
import tools.refinery.store.dse.modification.ModificationAdapter;
import tools.refinery.store.dse.propagation.PropagationAdapter;
import tools.refinery.store.dse.transition.DesignSpaceExplorationAdapter;
import tools.refinery.store.dse.transition.Rule;
import tools.refinery.store.query.interpreter.QueryInterpreterAdapter;
import tools.refinery.store.reasoning.ReasoningAdapter;
import tools.refinery.store.reasoning.actions.PartialActionLiterals;
import tools.refinery.store.reasoning.interpretation.PartialNeighborhoodCalculator;
import tools.refinery.store.reasoning.literal.Concreteness;
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

import static tools.refinery.logic.term.truthvalue.TruthValue.*;

public class StatechartMeasurement extends MeasurementBase {

	String fileNamePrefix = "statechart-";

	Set<Tuple> stateClassSet = new HashSet<>();
	Set<Tuple> transitionClassSet = new HashSet<>();

	Set<Tuple> outgoingTransitionSet = new HashSet<>();
	Set<Tuple> incomingTransitionSet = new HashSet<>();
	Set<Tuple> sourceSet = new HashSet<>();
	Set<Tuple> targetSet = new HashSet<>();

	Set<Tuple> activeSet = new HashSet<>();


	Map<String, Set<Tuple>> classMaps = new HashMap<>();
	Map<String, Set<Tuple>> relationMaps = new HashMap<>();
	Map<String, Set<Tuple>> attributeMaps = new HashMap<>();


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
					.containment(true)
					.opposite(source)
					.defaultValue(TruthValue.FALSE))
			.reference(incomingTransition, referenceBuilder -> referenceBuilder
					.source(stateClass)
					.target(transitionClass)
					.opposite(target)
					.defaultValue(TruthValue.FALSE))
			.reference(source, referenceBuilder -> referenceBuilder
					.source(transitionClass)
					.target(stateClass)
					.opposite(outgoingTransition)
					.defaultValue(TruthValue.FALSE))
			.reference(target, referenceBuilder -> referenceBuilder
					.source(transitionClass)
					.target(stateClass)
					.opposite(incomingTransition)
					.multiplicity(CardinalityIntervals.ONE, targetInvalidMultiplicity)
					.defaultValue(TruthValue.FALSE));

	Metamodel metamodel = builder.build();


    PartialRelation stateTransitionPreconditionPartialRelation = new PartialRelation("stateTransition_precondition", 3);
    TransitionRule stateTransition = new TransitionRule(stateTransitionPreconditionPartialRelation,
            Rule.of("stateTransition", (builder, t, from, to) -> builder
            .clause(
                    transitionClass.call(t),
					stateClass.call(from),
					stateClass.call(to),
                    source.call(t, from),
                    target.call(t, to),
					active.call(from)
            )
            .action(
					PartialActionLiterals.modify(active, FALSE, from),
					PartialActionLiterals.modify(active, TRUE, to)
            )));

    ModelSeed.Builder modelSeedBuilder;

	public void init(int size, MeasurementScenario scenario, boolean visualize) {
		this.visualize = visualize;
		this.scenario = scenario;
		this.size = size;

		if (visualize) {
			visualizationOutputPath = "output_visualization/statechart_" + size;
		}

		var filenameScenario = scenario.toString().toLowerCase();
		filenameScenario = switch (filenameScenario) {
//			case "query" -> "batch";
//			case "simulate" -> "batch";
			default -> "batch";
		};
		fileNamePrefix = fileNamePrefix + filenameScenario + "-" + size + "-";
		System.out.println(fileNamePrefix);

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
			// TODO
			storeBuilder = storeBuilder
					.with(TransitionSystemAdapter.builder());
		}
		case REPAIR -> {
			// TODO
			storeBuilder = storeBuilder
					.with(TransitionSystemAdapter.builder());
		}
		default -> throw new IllegalArgumentException("Invalid scenario for this measurement: " + scenario);
		}

		String pathToCsvFolder = "input/models/statechart_fast";

		for (var csvFile :
				Objects.requireNonNull(new File(pathToCsvFolder).listFiles((dir, name) -> name.startsWith(fileNamePrefix) &&
						name.endsWith(".csv") && classMaps.keySet().stream().anyMatch(name::contains)))) {
			String relationName = csvFile.getName().replace(fileNamePrefix, "").replace(".csv", "");
			System.out.println("Loading class: " + relationName);
			var inputMap = classMaps.get(relationName);
			try (java.io.BufferedReader br = new java.io.BufferedReader(new java.io.FileReader(csvFile))) {
				String line;
				while ((line = br.readLine()) != null) {
					String[] values = line.split(",");
					inputMap.add(Tuple.of(Integer.parseInt(values[0])));
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
					inputMap.add(Tuple.of(Integer.parseInt(values[0]), Integer.parseInt(values[1])));
				}
			} catch (java.io.IOException e) {
				e.printStackTrace();
			}
		}
		activeSet.add(Tuple.of(0)); // set the initial state as active

		var nodeCount = classMaps.values().stream().flatMap(Set::stream).mapToInt(t -> t.get(0)).max().orElse(0) + 1;
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
			Set<Tuple> tuples = entry.getValue();
			var partialRelation = switch (relationName) {
				case StateClassName -> stateClass;
				case TransitionClassName -> transitionClass;
				default -> throw new IllegalArgumentException("Unknown relation name: " + relationName);
			};
			modelSeedBuilder.seed(partialRelation, builder -> {
				for (var tuple : tuples) {
					builder.put(tuple, TRUE);
				}
				builder.reducedValue(UNKNOWN);
			});
		}


		for (var mapEntry : relationMaps.entrySet()) {
			String relationName = mapEntry.getKey();
			Set<Tuple> tuples = mapEntry.getValue();
			var partialRelation = switch (relationName) {
				case outgoingTransitionRelationName -> outgoingTransition;
				case sourceRelationName -> source;
				case targetRelationName -> target;
				default -> null;
			};
			if (partialRelation == null) {
				continue;
			}
			modelSeedBuilder.seed(partialRelation, builder -> {
				for (var tuple : tuples) {
					builder.put(tuple, TRUE);
				}
				builder.reducedValue(UNKNOWN);
			});
		}


		for (var mapEntry : relationMaps.entrySet()) {
			String relationName = mapEntry.getKey();
			Set<Tuple> tuples = mapEntry.getValue();
			var partialRelation = switch (relationName) {
				case incomingTransitionRelationName -> incomingTransition;
				default -> null;
			};
			if (partialRelation == null) {
				continue;
			}
			modelSeedBuilder.seed(partialRelation, builder -> {
				for (var tuple : tuples) {
					builder.put(tuple, TRUE);
				}
				builder.reducedValue(FALSE);
			});
		}


		for (var entry : attributeMaps.entrySet()) {
			String relationName = entry.getKey();
			Set<Tuple> tuples = entry.getValue();
			var partialRelation = switch (relationName) {
				case activeAttributeName -> active;
				default -> throw new IllegalArgumentException("Unknown relation name: " + relationName);
			};
			modelSeedBuilder.seed(partialRelation, builder -> {
				for (var tuple : tuples) {
					builder.put(tuple, TRUE);
				}
				builder.reducedValue(FALSE);
			});
		}


		switch (scenario) {
		case SIMULATE -> {
			modelSeedBuilder.seed(stateTransitionPreconditionPartialRelation, builder -> builder.reducedValue(UNKNOWN));
		}
		case INJECT -> {
		}
		case REPAIR -> {
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

