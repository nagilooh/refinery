package tools.refinery.measurement;

import tools.refinery.logic.dnf.Query;
import tools.refinery.logic.dnf.RelationalQuery;
import tools.refinery.logic.term.Variable;
import tools.refinery.logic.term.cardinalityinterval.CardinalityIntervals;
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
import tools.refinery.store.reasoning.representation.PartialSymbol;
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

public class StatechartAbstractModelCreator extends MeasurementBase {

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


	PartialRelation abstractTransitionToPreconditionPartialRelation = new PartialRelation(
			"abstractTransition_precondition", 3);
	TransitionRule abstractTransitionTo = new TransitionRule(abstractTransitionToPreconditionPartialRelation,
			Rule.of("abstractTransitionTo", (builder, t1, s2, s3) -> builder
					.clause((t2, s1) -> List.of(
							transitionClass.call(t1),
							transitionClass.call(t2),
							t1.notEquivalent(t2),
							stateClass.call(s1),
							stateClass.call(s2),
							stateClass.call(s3),
							s1.notEquivalent(s2),
							s1.notEquivalent(s3),
							s2.notEquivalent(s3),
							outgoingTransition.call(s1, t1),
							incomingTransition.call(s2, t1),
							outgoingTransition.call(s2, t2),
							incomingTransition.call(s3, t2)
					))
					.action(
							PartialActionLiterals.modify(incomingTransition, UNKNOWN, s2, t1),
							PartialActionLiterals.modify(incomingTransition, UNKNOWN, s3, t1)
					)));

	PartialRelation abstractTransitionFromPreconditionPartialRelation = new PartialRelation("abstractTransitionFrom_precondition", 3);
	TransitionRule abstractTransitionFrom = new TransitionRule(abstractTransitionFromPreconditionPartialRelation,
			Rule.of("abstractTransitionFrom", (builder, t2, s1, s2) -> builder
					.clause((t1, s3) -> List.of(
							transitionClass.call(t1),
							transitionClass.call(t2),
							t1.notEquivalent(t2),
							stateClass.call(s1),
							stateClass.call(s2),
							stateClass.call(s3),
							s1.notEquivalent(s2),
							s1.notEquivalent(s3),
							s2.notEquivalent(s3),
							outgoingTransition.call(s1, t1),
							incomingTransition.call(s2, t1),
							outgoingTransition.call(s2, t2),
							incomingTransition.call(s3, t2)
					))
					.action(
							PartialActionLiterals.modify(outgoingTransition, UNKNOWN, s1, t2),
							PartialActionLiterals.modify(outgoingTransition, UNKNOWN, s2, t2)
					)));

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


			storeBuilder = storeBuilder
					.with(TransitionSystemAdapter.builder()
							.transition(abstractTransitionTo)
							.transition(abstractTransitionFrom)
							.accept(_ -> () -> false) // full state space exploration
					)
					.with(new PredicateTranslator(
							abstractTransitionToPreconditionPartialRelation,
							abstractTransitionTo.rule().getPrecondition(),
							List.of(transitionClass, stateClass, stateClass), Set.of(), true, UNKNOWN))
					.with(new PredicateTranslator(
							abstractTransitionFromPreconditionPartialRelation,
							abstractTransitionFrom.rule().getPrecondition(),
							List.of(transitionClass, stateClass, stateClass), Set.of(), true, UNKNOWN));
			fireOneTransitionRules.add(abstractTransitionFrom.rule().getName());

		String pathToCsvFolder = "input/models/statechart_fast";

		for (var csvFile :
				Objects.requireNonNull(new File(pathToCsvFolder).listFiles((dir, name) -> name.startsWith(fileNamePrefix) &&
						name.endsWith(".csv") && classMaps.keySet().stream().anyMatch(name::contains)))) {
			String relationName = csvFile.getName().replace(fileNamePrefix, "").replace(".csv", "");
//			System.out.println("Loading class: " + relationName);
			var inputMap = classMaps.get(relationName);
//			System.out.println("Loading class: " + relationName + " from file: " + csvFile.getName());
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
//			System.out.println("Loading relation: " + relationName + " from file: " + csvFile.getName());
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

		modelSeedBuilder.seed(abstractTransitionToPreconditionPartialRelation,
				builder -> builder.reducedValue(UNKNOWN));
		modelSeedBuilder.seed(abstractTransitionFromPreconditionPartialRelation,
				builder -> builder.reducedValue(UNKNOWN));

		this.modelSeed = this.modelSeedBuilder.build();
		super.init();
    }

	public void saveModelToFile(String filename) {
		var reasoningAdapter = model.getAdapter(ReasoningAdapter.class);
		reasoningAdapter.getPartialSymbols().forEach(symbol -> {
//			If the symbol is contained in a list of symbols to ignore, skip it
			if (!List.of(stateClass, transitionClass, outgoingTransition, incomingTransition).contains(symbol)) {
				return;
			}
			var typedPartialSymbol = (PartialSymbol<?, ?>) symbol;
			var resultSet = reasoningAdapter.getPartialInterpretation(Concreteness.PARTIAL, typedPartialSymbol);

			var filenameScenario = scenario.toString().toLowerCase();
			filenameScenario = switch (filenameScenario) {
				case "query" -> "repair";
				case "simulate" -> "batch";
				default -> filenameScenario;
			};
			var outputFile = new File(filename + "-" + filenameScenario + "-abstract-" + size + "-" + symbol.name() +
					".csv");
			try (java.io.BufferedWriter writer = new java.io.BufferedWriter(new java.io.FileWriter(outputFile))) {
				var cursor = resultSet.getAll();
				while (cursor.move()) {
					writer.write(cursor.getKey().toString().replace(" ", "").replace("[", "").replace("]", "") + "," + cursor.getValue().toString());
					writer.newLine();
				}
			} catch (java.io.IOException e) {
				e.printStackTrace();
			}
		});
	}

	@Override
	protected boolean renderTransitionsToAlreadyVisitedStates() {
		return true;
	}
}

