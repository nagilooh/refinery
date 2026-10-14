package tools.refinery.measurement;

import tools.refinery.logic.term.cardinalityinterval.CardinalityIntervals;
import tools.refinery.logic.term.intinterval.IntInterval;
import tools.refinery.logic.term.intinterval.IntIntervalDomain;
import tools.refinery.logic.term.truthvalue.TruthValue;
import tools.refinery.store.dse.modification.ModificationAdapter;
import tools.refinery.store.dse.propagation.PropagationAdapter;
import tools.refinery.store.dse.transition.DesignSpaceExplorationAdapter;
import tools.refinery.store.query.interpreter.QueryInterpreterAdapter;
import tools.refinery.store.reasoning.ReasoningAdapter;
import tools.refinery.store.reasoning.interpretation.PartialNeighborhoodCalculator;
import tools.refinery.store.reasoning.literal.Concreteness;
import tools.refinery.store.reasoning.representation.PartialFunction;
import tools.refinery.store.reasoning.representation.PartialRelation;
import tools.refinery.store.reasoning.representation.PartialSymbol;
import tools.refinery.store.reasoning.seed.ModelSeed;
import tools.refinery.store.reasoning.translator.ConcretizationSettings;
import tools.refinery.store.reasoning.translator.attribute.AttributeInfo;
import tools.refinery.store.reasoning.translator.containment.ContainmentHierarchyTranslator;
import tools.refinery.store.reasoning.translator.metamodel.Metamodel;
import tools.refinery.store.reasoning.translator.metamodel.MetamodelBuilder;
import tools.refinery.store.reasoning.translator.metamodel.MetamodelTranslator;
import tools.refinery.store.reasoning.translator.multiobject.MultiObjectTranslator;
import tools.refinery.store.reasoning.translator.predicate.BasePredicateTranslator;
import tools.refinery.store.statecoding.StateCoderAdapter;
import tools.refinery.store.transition.system.TransitionSystemAdapter;
import tools.refinery.store.tuple.Tuple;

import java.io.File;
import java.math.BigInteger;
import java.util.*;

import static tools.refinery.logic.term.truthvalue.TruthValue.*;

public class TrainBenchmarkMeasurement extends MeasurementBase {

	Set<Tuple> regionClassSet = new HashSet<>();
	Set<Tuple> semaphoreClassSet = new HashSet<>();
	Set<Tuple> routeClassSet = new HashSet<>();
	Set<Tuple> switchPositionClassSet = new HashSet<>();
	Set<Tuple> sensorClassSet = new HashSet<>();
	Set<Tuple> segmentClassSet = new HashSet<>();
	Set<Tuple> switchClassSet = new HashSet<>();
	Set<Tuple> railwayContainerClassSet = new HashSet<>();
	Set<Tuple> trackElementClassSet = new HashSet<>();

	Set<Tuple> sensorsMap = new HashSet<>();
	Set<Tuple> elementsMap = new HashSet<>();
	Set<Tuple> followsMap = new HashSet<>();
	Set<Tuple> requiresMap = new HashSet<>();
	Set<Tuple> entryMap = new HashSet<>();
	Set<Tuple> exitMap = new HashSet<>();
	Set<Tuple> routeMap = new HashSet<>();
	Set<Tuple> targetMap = new HashSet<>();
	Set<Tuple> monitoredByMap = new HashSet<>();
	Set<Tuple> connectsToMap = new HashSet<>();
	Set<Tuple> semaphoresMap = new HashSet<>();
	Set<Tuple> straightMap = new HashSet<>();
	Set<Tuple> divergingMap = new HashSet<>();
	Set<Tuple> monitorsMap = new HashSet<>();
	Set<Tuple> positionsMap = new HashSet<>();
	Set<Tuple> regionsMap = new HashSet<>();
	Set<Tuple> routesMap = new HashSet<>();
	Map<Tuple, IntInterval> lengthMap = new HashMap<>();

	Set<Tuple> signalMap = new HashSet<>();
	Set<Tuple> positionMap = new HashSet<>();
	Set<Tuple> currentPositionMap = new HashSet<>();

	Map<Tuple, TruthValue> activeMap = new HashMap<>();

	Map<String, Tuple> positionEnumMap = new HashMap<>();
	Map<String, Tuple> signalEnumMap = new HashMap<>();

	Map<String, Set<Tuple>> classMaps = new HashMap<>();
	Map<String, Set<Tuple>> relationMaps = new HashMap<>();
	Map<String, Map<String, Tuple>> enumMaps = new HashMap<>();
	Map<String, Set<Tuple>> enumAttributeMaps = new HashMap<>();
	Map<String, Map<Tuple, TruthValue>> booleanAttributeMaps = new HashMap<>();
	Map<String, Map<Tuple, IntInterval>> intAttributeMaps = new HashMap<>();

//	Classes
	PartialRelation railwayContainerClass = new PartialRelation("RailwayContainer", 1);
	PartialRelation regionClass = new PartialRelation("Region", 1);
	PartialRelation semaphoreClass = new PartialRelation("Semaphore", 1);
	PartialRelation routeClass = new PartialRelation("Route", 1);
	PartialRelation switchPositionClass = new PartialRelation("SwitchPosition", 1);
	PartialRelation trackElementClass = new PartialRelation("TrackElement", 1);
	PartialRelation sensorClass = new PartialRelation("Sensor", 1);
	PartialRelation segmentClass = new PartialRelation("Segment", 1);
	PartialRelation switchClass = new PartialRelation("Switch", 1);

//	Enums
	PartialRelation positionEnum = new PartialRelation("Position", 1);
	PartialRelation signalEnum = new PartialRelation("Signal", 1);

//	References
	PartialRelation routes = new PartialRelation("routes", 2);
	PartialRelation regions = new PartialRelation("regions", 2);
	PartialRelation sensors = new PartialRelation("sensors", 2);
	PartialRelation elements = new PartialRelation("elements", 2);
	PartialRelation signal = new PartialRelation("signal", 2);
	PartialRelation follows = new PartialRelation("follows", 2);
	PartialRelation requires = new PartialRelation("requires", 2);
	PartialRelation entry = new PartialRelation("entry", 2);
	PartialRelation exit = new PartialRelation("exit", 2);
	PartialRelation route = new PartialRelation("route", 2);
	PartialRelation target = new PartialRelation("target", 2);
	PartialRelation position = new PartialRelation("position", 2);
	PartialRelation monitoredBy = new PartialRelation("monitoredBy", 2);
	PartialRelation connectsTo = new PartialRelation("connectsTo", 2);
	PartialRelation monitors = new PartialRelation("monitors", 2);
	PartialRelation semaphores = new PartialRelation("semaphores", 2);
	PartialRelation positions = new PartialRelation("positions", 2);
	PartialRelation currentPosition = new PartialRelation("currentPosition", 2);
	PartialRelation straight = new PartialRelation("straight", 2);
	PartialRelation diverging = new PartialRelation("diverging", 2);


	PartialRelation signalInvalidMultiplicity = new PartialRelation("signal::invalidMultiplicity", 1);
	PartialRelation requiresInvalidMultiplicity = new PartialRelation("requires::invalidMultiplicity", 1);
	PartialRelation entryInvalidMultiplicity = new PartialRelation("entry::invalidMultiplicity", 1);
	PartialRelation exitInvalidMultiplicity = new PartialRelation("exit::invalidMultiplicity", 1);
	PartialRelation routeInvalidMultiplicity = new PartialRelation("route::invalidMultiplicity", 1);
	PartialRelation targetInvalidMultiplicity = new PartialRelation("target::invalidMultiplicity", 1);
	PartialRelation positionInvalidMultiplicity = new PartialRelation("position::invalidMultiplicity", 1);
	PartialRelation currentPositionInvalidMultiplicity = new PartialRelation("currentPosition::invalidMultiplicity", 1);
	PartialRelation straightInvalidMultiplicity = new PartialRelation("straight::invalidMultiplicity", 1);
	PartialRelation divergingInvalidMultiplicity = new PartialRelation("diverging::invalidMultiplicity", 1);


	PartialFunction<IntInterval, BigInteger> length = PartialSymbol.of("length", 1, IntIntervalDomain.INSTANCE);

	PartialRelation active = new PartialRelation("active", 1);


	MetamodelBuilder builder = Metamodel.builder()
			.type(railwayContainerClass)
			.type(regionClass)
			.type(semaphoreClass)
			.type(routeClass)
			.type(switchPositionClass)
			.type(trackElementClass, true, Set.of())
			.type(sensorClass)
			.type(segmentClass, trackElementClass)
			.type(switchClass, trackElementClass)

			.type(positionEnum)
			.type(signalEnum)

			.attribute(length, new AttributeInfo(segmentClass, IntInterval.UNKNOWN))

			.reference(routes, referenceBuilder -> referenceBuilder
					.source(railwayContainerClass)
					.target(routeClass)
					.containment(true)
					.defaultValue(FALSE))
			.reference(regions, referenceBuilder -> referenceBuilder
					.source(railwayContainerClass)
					.target(regionClass)
					.containment(true)
					.defaultValue(FALSE))
			.reference(sensors, referenceBuilder -> referenceBuilder
					.source(regionClass)
					.target(sensorClass)
					.containment(true)
					.defaultValue(FALSE))
			.reference(elements, referenceBuilder -> referenceBuilder
					.source(regionClass)
					.target(trackElementClass)
					.containment(true)
					.defaultValue(FALSE))
			.reference(signal, referenceBuilder -> referenceBuilder
					.source(semaphoreClass)
					.target(signalEnum)
					.multiplicity(CardinalityIntervals.LONE, signalInvalidMultiplicity)
					.defaultValue(FALSE))
			.reference(follows, referenceBuilder -> referenceBuilder
					.source(routeClass)
					.target(switchPositionClass)
					.opposite(route)
					.defaultValue(FALSE))
			.reference(requires, referenceBuilder -> referenceBuilder
					.source(routeClass)
					.target(sensorClass)
					.multiplicity(CardinalityIntervals.atLeast(2), requiresInvalidMultiplicity)
					.defaultValue(FALSE))
			.reference(entry, referenceBuilder -> referenceBuilder
					.source(routeClass)
					.target(semaphoreClass)
					.multiplicity(CardinalityIntervals.LONE, entryInvalidMultiplicity)
					.defaultValue(FALSE))
			.reference(exit, referenceBuilder -> referenceBuilder
					.source(routeClass)
					.target(semaphoreClass)
					.multiplicity(CardinalityIntervals.LONE, exitInvalidMultiplicity)
					.defaultValue(FALSE))
			.reference(route, referenceBuilder -> referenceBuilder
					.source(switchPositionClass)
					.target(routeClass)
					.opposite(follows)
					.multiplicity(CardinalityIntervals.LONE, routeInvalidMultiplicity)
					.defaultValue(FALSE))
			.reference(target, referenceBuilder -> referenceBuilder
					.source(switchPositionClass)
					.target(switchClass)
					.opposite(positions)
					.multiplicity(CardinalityIntervals.ONE, targetInvalidMultiplicity)
					.defaultValue(FALSE))
			.reference(position, referenceBuilder -> referenceBuilder
					.source(switchPositionClass)
					.target(positionEnum)
					.multiplicity(CardinalityIntervals.LONE, positionInvalidMultiplicity)
					.defaultValue(FALSE))
			.reference(monitoredBy, referenceBuilder -> referenceBuilder
					.source(trackElementClass)
					.target(sensorClass)
					.opposite(monitors)
					.defaultValue(FALSE))
			.reference(connectsTo, referenceBuilder -> referenceBuilder
					.source(trackElementClass)
					.target(trackElementClass)
					.defaultValue(FALSE))
			.reference(monitors, referenceBuilder -> referenceBuilder
					.source(sensorClass)
					.target(trackElementClass)
					.opposite(monitoredBy)
					.defaultValue(FALSE))
			.reference(semaphores, referenceBuilder -> referenceBuilder
					.source(segmentClass)
					.target(semaphoreClass)
					.containment(true)
					.defaultValue(FALSE))
			.reference(positions, referenceBuilder -> referenceBuilder
					.source(switchClass)
					.target(switchPositionClass)
					.opposite(target)
					.defaultValue(FALSE))
			.reference(currentPosition, referenceBuilder -> referenceBuilder
					.source(switchClass)
					.target(positionEnum)
					.multiplicity(CardinalityIntervals.LONE, currentPositionInvalidMultiplicity)
					.defaultValue(FALSE))
			.reference(straight, referenceBuilder -> referenceBuilder
					.source(switchClass)
					.target(trackElementClass)
					.multiplicity(CardinalityIntervals.LONE, straightInvalidMultiplicity)
					.defaultValue(FALSE))
			.reference(diverging, referenceBuilder -> referenceBuilder
					.source(switchClass)
					.target(trackElementClass)
					.multiplicity(CardinalityIntervals.LONE, divergingInvalidMultiplicity)
					.defaultValue(FALSE));

	Metamodel metamodel = builder.build();

    ModelSeed.Builder modelSeedBuilder;

	public void init(int size, boolean visualize) {
		this.visualize = visualize;
		if (visualize) {
			visualizationOutputPath = "output_visualization/trainbenchmark_" + size;
		}

		classMaps.put("Region", regionClassSet);
		classMaps.put("Semaphore", semaphoreClassSet);
		classMaps.put("Route", routeClassSet);
		classMaps.put("SwitchPosition", switchPositionClassSet);
		classMaps.put("Sensor", sensorClassSet);
		classMaps.put("Segment", segmentClassSet);
		classMaps.put("Switch", switchClassSet);
		classMaps.put("RailwayContainerClass", railwayContainerClassSet);
		classMaps.put("TrackElement", trackElementClassSet);
		relationMaps.put("sensors", sensorsMap);
		relationMaps.put("elements", elementsMap);
		relationMaps.put("follows", followsMap);
		relationMaps.put("requires", requiresMap);
		relationMaps.put("entry", entryMap);
		relationMaps.put("exit", exitMap);
		relationMaps.put("route", routeMap);
		relationMaps.put("target", targetMap);
		relationMaps.put("monitoredBy", monitoredByMap);
		relationMaps.put("connectsTo", connectsToMap);
		relationMaps.put("semaphores", semaphoresMap);
		relationMaps.put("straight", straightMap);
		relationMaps.put("diverging", divergingMap);
		relationMaps.put("monitors", monitorsMap);
		relationMaps.put("positions", positionsMap);
		relationMaps.put("regions", regionsMap);
		relationMaps.put("routes", routesMap);

		enumMaps.put("Signal", signalEnumMap);
		enumMaps.put("Position", positionEnumMap);
		enumAttributeMaps.put("signal", signalMap);
		enumAttributeMaps.put("position", positionMap);
		enumAttributeMaps.put("currentPosition", currentPositionMap);

		intAttributeMaps.put("length", lengthMap);

		booleanAttributeMaps.put("active", activeMap);

		storeBuilder = storeBuilder
				.with(QueryInterpreterAdapter.builder())
				.with(PropagationAdapter.builder())
				.with(StateCoderAdapter.builder()
						.stateCodeCalculatorFactory(PartialNeighborhoodCalculator.factory(Concreteness.PARTIAL, 7))
				)
				.with(ModificationAdapter.builder())
				.with(DesignSpaceExplorationAdapter.builder())
				.with(ReasoningAdapter.builder())
				.with(TransitionSystemAdapter.builder()
						.accept(_ -> () -> false) // full state space exploration
				)
				.with(new MultiObjectTranslator())
				.with(new MetamodelTranslator(metamodel))
				.with(new BasePredicateTranslator(active, List.of(routeClass), Set.of(), UNKNOWN,
						new ConcretizationSettings(true, true)));

		String pathToCsvFolder = "input/models/trainbenchmark/size-1"; // Replace with the actual path to your


		for (var csvFile :
				new File(pathToCsvFolder).listFiles((dir, name) -> name.startsWith("railway-batch-1") &&
						name.endsWith(".csv") && classMaps.keySet().stream().anyMatch(name::contains))) {
			String relationName = csvFile.getName().replace("railway-batch-1-", "").replace(".csv", "");
			System.out.println("Loading CSV file: " + csvFile.getName() + " for relation: " + relationName);
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
				new File(pathToCsvFolder).listFiles((dir, name) -> name.startsWith("railway-batch-1") &&
						name.endsWith(".csv") && relationMaps.keySet().stream().anyMatch(name::contains))) {
			String relationName = csvFile.getName().replace("railway-batch-1-", "").replace(".csv", "");
			System.out.println("Loading CSV file: " + csvFile.getName() + " for relation: " + relationName);
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
		for (var csvFile :
				new File(pathToCsvFolder).listFiles((dir, name) -> name.startsWith("railway-batch-1") &&
						name.endsWith(".csv") && booleanAttributeMaps.keySet().stream().anyMatch(name::contains))) {
			String relationName = csvFile.getName().replace("railway-batch-1-", "").replace(".csv", "");
			System.out.println("Loading CSV file: " + csvFile.getName() + " for relation: " + relationName);
			var inputMap = booleanAttributeMaps.get(relationName);
			try (java.io.BufferedReader br = new java.io.BufferedReader(new java.io.FileReader(csvFile))) {
				String line;
				while ((line = br.readLine()) != null) {
					String[] values = line.split(",");
					inputMap.put(Tuple.of(Integer.parseInt(values[0])), of(Boolean.parseBoolean(values[1])));
				}
			} catch (java.io.IOException e) {
				e.printStackTrace();
			}
		}
		for (var csvFile :
				new File(pathToCsvFolder).listFiles((dir, name) -> name.startsWith("railway-batch-1") &&
						name.endsWith(".csv") && intAttributeMaps.keySet().stream().anyMatch(name::contains))) {
			String relationName = csvFile.getName().replace("railway-batch-1-", "").replace(".csv", "");
			System.out.println("Loading CSV file: " + csvFile.getName() + " for relation: " + relationName);
			var inputMap = intAttributeMaps.get(relationName);
			try (java.io.BufferedReader br = new java.io.BufferedReader(new java.io.FileReader(csvFile))) {
				String line;
				while ((line = br.readLine()) != null) {
					String[] values = line.split(",");
					inputMap.put(Tuple.of(Integer.parseInt(values[0])), IntInterval.of(Integer.parseInt(values[1])));
				}
			} catch (java.io.IOException e) {
				e.printStackTrace();
			}
		}
		var nextId = classMaps.values().stream().flatMap(Set::stream).mapToInt(t -> t.get(0)).max().orElse(0) + 1;
		signalEnumMap.put("Signal::FAILURE", Tuple.of(nextId++));
		signalEnumMap.put("Signal::STOP", Tuple.of(nextId++));
		signalEnumMap.put("Signal::GO", Tuple.of(nextId++));
		positionEnumMap.put("Position::FAILURE", Tuple.of(nextId++));
		positionEnumMap.put("Position::STRAIGHT", Tuple.of(nextId++));
		positionEnumMap.put("Position::DIVERGING", Tuple.of(nextId++));
		railwayContainerClassSet.add(Tuple.of(nextId++));
		for (var csvFile :
				new File(pathToCsvFolder).listFiles((dir, name) -> name.startsWith("railway-batch-1") &&
						name.endsWith(".csv") && enumAttributeMaps.keySet().stream().anyMatch(name::contains))) {
			String relationName = csvFile.getName().replace("railway-batch-1-", "").replace(".csv", "");
			System.out.println("Loading CSV file: " + csvFile.getName() + " for relation: " + relationName);
			var inputMap = enumAttributeMaps.get(relationName);
			try (java.io.BufferedReader br = new java.io.BufferedReader(new java.io.FileReader(csvFile))) {
				String line;
				while ((line = br.readLine()) != null) {
					String[] values = line.split(",");
					var enumValue = signalEnumMap.get(values[1]);
					if (enumValue == null) {
						enumValue = positionEnumMap.get(values[1]);
					}
					if (enumValue == null) {
						throw new IllegalArgumentException("Invalid enum value: " + values[1]);
					}
					inputMap.add(Tuple.of(Integer.parseInt(values[0]), enumValue.get(0)));
				}
			} catch (java.io.IOException e) {
				e.printStackTrace();
			}
		}

////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

		var nodeCount = nextId;
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
				case "Region" -> regionClass;
				case "Semaphore" -> semaphoreClass;
				case "Route" -> routeClass;
				case "SwitchPosition" -> switchPositionClass;
				case "Sensor" -> sensorClass;
				case "Segment" -> segmentClass;
				case "Switch" -> switchClass;
				case "RailwayContainerClass" -> railwayContainerClass;
				case "TrackElement" -> trackElementClass;
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
				case "sensors" -> sensors;
				case "elements" -> elements;
				case "follows" -> follows;
				case "requires" -> requires;
				case "entry" -> entry;
				case "exit" -> exit;
				case "route" -> route;
				case "target" -> target;
				case "monitoredBy" -> monitoredBy;
				case "connectsTo" -> connectsTo;
				case "semaphores" -> semaphores;
				case "straight" -> straight;
				case "diverging" -> diverging;
				case "monitors" -> monitors;
				case "positions" -> positions;
				case "regions" -> regions;
				case "routes" -> routes;
				default -> throw new IllegalArgumentException("Unknown relation name: " + relationName);
			};
			modelSeedBuilder.seed(partialRelation, builder -> {
				for (var tuple : tuples) {
					builder.put(tuple, TRUE);
				}
				builder.reducedValue(FALSE);
			});
		}

		for (var entry : enumMaps.entrySet()) {
			String relationName = entry.getKey();
			Map<String, Tuple> tuples = entry.getValue();
			var partialRelation = switch (relationName) {
				case "Signal" -> signalEnum;
				case "Position" -> positionEnum;
				default -> throw new IllegalArgumentException("Unknown relation name: " + relationName);
			};
			modelSeedBuilder.seed(partialRelation, builder -> {
				for (var tupleEntry : tuples.entrySet()) {
					builder.put(tupleEntry.getValue(), TRUE);
				}
				builder.reducedValue(UNKNOWN);
			});
		}

		for (var mapEntry : enumAttributeMaps.entrySet()) {
			String relationName = mapEntry.getKey();
			Set<Tuple> tuples = mapEntry.getValue();
			var partialRelation = switch (relationName) {
				case "signal" -> signal;
				case "position" -> position;
				case "currentPosition" -> currentPosition;
				default -> throw new IllegalArgumentException("Unknown relation name: " + relationName);
			};
			modelSeedBuilder.seed(partialRelation, builder -> {
				for (var tuple : tuples) {
					builder.put(tuple, TRUE);
				}
				builder.reducedValue(FALSE);
			});
		}

		for (var mapEntry : booleanAttributeMaps.entrySet()) {
			String relationName = mapEntry.getKey();
			Map<Tuple, TruthValue> tuples = mapEntry.getValue();
			if (!relationName.equals("active")) {
				throw new IllegalArgumentException("Unknown relation name: " + relationName);
			}
			var partialRelation = active;
			modelSeedBuilder.seed(partialRelation, builder -> {
				for (var tupleEntry : tuples.entrySet()) {
					builder.put(tupleEntry.getKey(), tupleEntry.getValue());
				}
				builder.reducedValue(FALSE);
			});
		}

		for (var mapEntry : intAttributeMaps.entrySet()) {
			String relationName = mapEntry.getKey();
			Map<Tuple, IntInterval> tuples = mapEntry.getValue();
			if (!relationName.equals("length")) {
				throw new IllegalArgumentException("Unknown relation name: " + relationName);
			}
			var partialRelation = length;
			modelSeedBuilder.seed(partialRelation, builder -> {
				for (var tupleEntry : tuples.entrySet()) {
					builder.put(tupleEntry.getKey(), tupleEntry.getValue());
				}
				builder.reducedValue(IntInterval.UNKNOWN);
			});
		}
        this.modelSeed = this.modelSeedBuilder.build();
		super.init();
    }

	@Override
	protected boolean renderTransitionsToAlreadyVisitedStates() {
		return true;
	}
}

