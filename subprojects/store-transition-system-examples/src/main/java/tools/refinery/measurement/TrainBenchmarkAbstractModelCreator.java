package tools.refinery.measurement;

import tools.refinery.logic.dnf.Query;
import tools.refinery.logic.dnf.QueryBuilder;
import tools.refinery.logic.dnf.RelationalQuery;
import tools.refinery.logic.term.NodeVariable;
import tools.refinery.logic.term.Variable;
import tools.refinery.logic.term.abstractdomain.AbstractDomainTerms;
import tools.refinery.logic.term.cardinalityinterval.CardinalityIntervals;
import tools.refinery.logic.term.intinterval.IntInterval;
import tools.refinery.logic.term.intinterval.IntIntervalDomain;
import tools.refinery.logic.term.intinterval.IntIntervalTerms;
import tools.refinery.logic.term.truthvalue.TruthValue;
import tools.refinery.store.dse.modification.ModificationAdapter;
import tools.refinery.store.dse.propagation.PropagationAdapter;
import tools.refinery.store.dse.transition.DesignSpaceExplorationAdapter;
import tools.refinery.store.dse.transition.Rule;
import tools.refinery.store.dse.transition.RuleBuilder;
import tools.refinery.store.dse.transition.objectives.Criterion;
import tools.refinery.store.dse.transition.statespace.ActivationStore;
import tools.refinery.store.map.Version;
import tools.refinery.store.query.ModelQueryAdapter;
import tools.refinery.store.query.interpreter.QueryInterpreterAdapter;
import tools.refinery.store.reasoning.ReasoningAdapter;
import tools.refinery.store.reasoning.ReasoningBuilder;
import tools.refinery.store.reasoning.actions.CleanupActionLiteral;
import tools.refinery.store.reasoning.actions.CreateNewActionLiteral;
import tools.refinery.store.reasoning.actions.PartialActionLiterals;
import tools.refinery.store.reasoning.interpretation.PartialNeighborhoodCalculator;
import tools.refinery.store.reasoning.literal.Concreteness;
import tools.refinery.store.reasoning.literal.Modality;
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
import tools.refinery.store.reasoning.translator.predicate.PredicateTranslator;
import tools.refinery.store.statecoding.StateCoderAdapter;
import tools.refinery.store.transition.system.TransitionSystemAdapter;
import tools.refinery.store.transition.system.statespace.TransitionRule;
import tools.refinery.store.tuple.Tuple;

import java.io.File;
import java.math.BigInteger;
import java.util.*;
import java.util.stream.IntStream;

import static tools.refinery.logic.literal.Literals.not;
import static tools.refinery.logic.term.truthvalue.TruthValue.*;
import static tools.refinery.store.reasoning.literal.PartialLiterals.must;
import static tools.refinery.store.reasoning.literal.PartialLiterals.partialCheck;

public class TrainBenchmarkAbstractModelCreator extends MeasurementBase {

	String fileNamePrefix = "railway-";

	int firstFreeId = 0;
	int POSITION_FAILURE = firstFreeId++;
	int POSITION_STRAIGHT = firstFreeId++;
	int POSITION_DIVERGING = firstFreeId++;
	int SIGNAL_FAILURE = firstFreeId++;
	int SIGNAL_STOP = firstFreeId++;
	int SIGNAL_GO = firstFreeId++;

	Map<Tuple, TruthValue> regionClassSet = new HashMap<>();
	Map<Tuple, TruthValue> semaphoreClassSet = new HashMap<>();
	Map<Tuple, TruthValue> routeClassSet = new HashMap<>();
	Map<Tuple, TruthValue> switchPositionClassSet = new HashMap<>();
	Map<Tuple, TruthValue> sensorClassSet = new HashMap<>();
	Map<Tuple, TruthValue> segmentClassSet = new HashMap<>();
	Map<Tuple, TruthValue> switchClassSet = new HashMap<>();
	Map<Tuple, TruthValue> railwayContainerClassSet = new HashMap<>();
	Map<Tuple, TruthValue> trackElementClassSet = new HashMap<>();
	Map<Tuple, TruthValue> trainClassSet = new HashMap<>();

	Map<Tuple, TruthValue> sensorsMap = new HashMap<>();
	Map<Tuple, TruthValue> elementsMap = new HashMap<>();
	Map<Tuple, TruthValue> followsMap = new HashMap<>();
	Map<Tuple, TruthValue> requiresMap = new HashMap<>();
	Map<Tuple, TruthValue> entryMap = new HashMap<>();
	Map<Tuple, TruthValue> exitMap = new HashMap<>();
	Map<Tuple, TruthValue> routeMap = new HashMap<>();
	Map<Tuple, TruthValue> targetMap = new HashMap<>();
	Map<Tuple, TruthValue> monitoredByMap = new HashMap<>();
	Map<Tuple, TruthValue> connectsToMap = new HashMap<>();
	Map<Tuple, TruthValue> semaphoresMap = new HashMap<>();
	Map<Tuple, TruthValue> straightMap = new HashMap<>();
	Map<Tuple, TruthValue> divergingMap = new HashMap<>();
	Map<Tuple, TruthValue> monitorsMap = new HashMap<>();
	Map<Tuple, TruthValue> positionsMap = new HashMap<>();
	Map<Tuple, TruthValue> regionsMap = new HashMap<>();
	Map<Tuple, TruthValue> routesMap = new HashMap<>();
	Map<Tuple, TruthValue> locationMap = new HashMap<>();
	Map<Tuple, IntInterval> lengthMap = new HashMap<>();

	Map<Tuple, TruthValue> signalMap = new HashMap<>();
	Map<Tuple, TruthValue> positionMap = new HashMap<>();
	Map<Tuple, TruthValue> currentPositionMap = new HashMap<>();

	Map<Tuple, TruthValue> activeMap = new HashMap<>();

	Map<String, Tuple> positionEnumMap = new HashMap<>();
	Map<String, Tuple> signalEnumMap = new HashMap<>();

	Map<String, Map<Tuple, TruthValue>> classMaps = new HashMap<>();
	Map<String, Map<Tuple, TruthValue>> relationMaps = new HashMap<>();
	Map<String, Map<String, Tuple>> enumMaps = new HashMap<>();
	Map<String, Map<Tuple, TruthValue>> enumAttributeMaps = new HashMap<>();
	Map<String, Map<Tuple, TruthValue>> booleanAttributeMaps = new HashMap<>();
	Map<String, Map<Tuple, IntInterval>> intAttributeMaps = new HashMap<>();

	static final String RailwayContainerClassName = "RailwayContainer";
	static final String RegionClassName = "Region";
	static final String SemaphoreClassName = "Semaphore";
	static final String RouteClassName = "Route";
	static final String SwitchPositionClassName = "SwitchPosition";
	static final String TrackElementClassName = "TrackElement";
	static final String SensorClassName = "Sensor";
	static final String SegmentClassName = "Segment";
	static final String SwitchClassName = "Switch";
	static final String PositionEnumName = "Position";
	static final String SignalEnumName = "Signal";
	static final String TrainClassName = "Train";

	static final String routesRelationName = "routes";
	static final String regionsRelationName = "regions";
	static final String sensorsRelationName = "sensors";
	static final String elementsRelationName = "elements";
	static final String signalRelationName = "signal";
	static final String followsRelationName = "follows";
	static final String requiresRelationName = "requires";
	static final String entryRelationName = "entry";
	static final String exitRelationName = "exit";
	static final String routeRelationName = "route";
	static final String targetRelationName = "target";
	static final String positionRelationName = "position";
	static final String monitoredByRelationName = "monitoredBy";
	static final String connectsToRelationName = "connectsTo";
	static final String monitorsRelationName = "monitors";
	static final String semaphoresRelationName = "semaphores";
	static final String positionsRelationName = "positions";
	static final String currentPositionRelationName = "currentPosition";
	static final String straightRelationName = "straight";
	static final String divergingRelationName = "diverging";
	static final String locationRelationName = "location";

	static final String lengthAttributeName = "length";
	static final String activeAttributeName = "active";

//	Classes
	PartialRelation railwayContainerClass = new PartialRelation(RailwayContainerClassName, 1);
	PartialRelation regionClass = new PartialRelation(RegionClassName, 1);
	PartialRelation semaphoreClass = new PartialRelation(SemaphoreClassName, 1);
	PartialRelation routeClass = new PartialRelation(RouteClassName, 1);
	PartialRelation switchPositionClass = new PartialRelation(SwitchPositionClassName, 1);
	PartialRelation trackElementClass = new PartialRelation(TrackElementClassName, 1);
	PartialRelation sensorClass = new PartialRelation(SensorClassName, 1);
	PartialRelation segmentClass = new PartialRelation(SegmentClassName, 1);
	PartialRelation switchClass = new PartialRelation(SwitchClassName, 1);
	PartialRelation trainClass = new PartialRelation(TrainClassName, 1);

//	Enums
	PartialRelation positionEnum = new PartialRelation(PositionEnumName, 1);
	PartialRelation signalEnum = new PartialRelation(SignalEnumName, 1);

//	References
	PartialRelation routes = new PartialRelation(routesRelationName, 2);
	PartialRelation regions = new PartialRelation(regionsRelationName, 2);
	PartialRelation sensors = new PartialRelation(sensorsRelationName, 2);
	PartialRelation elements = new PartialRelation(elementsRelationName, 2);
	PartialRelation signal = new PartialRelation(signalRelationName, 2);
	PartialRelation follows = new PartialRelation(followsRelationName, 2);
	PartialRelation requires = new PartialRelation(requiresRelationName, 2);
	PartialRelation entry = new PartialRelation(entryRelationName, 2);
	PartialRelation exit = new PartialRelation(exitRelationName, 2);
	PartialRelation route = new PartialRelation(routeRelationName, 2);
	PartialRelation target = new PartialRelation(targetRelationName, 2);
	PartialRelation position = new PartialRelation(positionRelationName, 2);
	PartialRelation monitoredBy = new PartialRelation(monitoredByRelationName, 2);
	PartialRelation connectsTo = new PartialRelation(connectsToRelationName, 2);
	PartialRelation monitors = new PartialRelation(monitorsRelationName, 2);
	PartialRelation semaphores = new PartialRelation(semaphoresRelationName, 2);
	PartialRelation positions = new PartialRelation(positionsRelationName, 2);
	PartialRelation currentPosition = new PartialRelation(currentPositionRelationName, 2);
	PartialRelation straight = new PartialRelation(straightRelationName, 2);
	PartialRelation diverging = new PartialRelation(divergingRelationName, 2);
	PartialRelation location = new PartialRelation(locationRelationName, 2);


	PartialRelation signalInvalidMultiplicity = new PartialRelation("signal::invalidMultiplicity", 1);
	PartialRelation requiresInvalidMultiplicity = new PartialRelation("requires::invalidMultiplicity", 1);
	PartialRelation entryInvalidMultiplicity = new PartialRelation("entry::invalidMultiplicity", 1);
	PartialRelation exitInvalidMultiplicity = new PartialRelation("exit::invalidMultiplicity", 1);
	PartialRelation targetInvalidMultiplicity = new PartialRelation("target::invalidMultiplicity", 1);
	PartialRelation positionInvalidMultiplicity = new PartialRelation("position::invalidMultiplicity", 1);
	PartialRelation currentPositionInvalidMultiplicity = new PartialRelation("currentPosition::invalidMultiplicity", 1);
	PartialRelation straightInvalidMultiplicity = new PartialRelation("straight::invalidMultiplicity", 1);
	PartialRelation divergingInvalidMultiplicity = new PartialRelation("diverging::invalidMultiplicity", 1);
	PartialRelation locationInvalidMultiplicity = new PartialRelation("location::invalidMultiplicity", 1);


	PartialFunction<IntInterval, BigInteger> length = PartialSymbol.of(lengthAttributeName, 1, IntIntervalDomain.INSTANCE);

	PartialRelation active = new PartialRelation(activeAttributeName, 1);


	MetamodelBuilder metamodelBuilder = Metamodel.builder()
			.type(railwayContainerClass)
			.type(regionClass)
			.type(semaphoreClass)
			.type(routeClass)
			.type(switchPositionClass)
			.type(trackElementClass, true, Set.of())
			.type(sensorClass)
			.type(segmentClass, trackElementClass)
			.type(switchClass, trackElementClass)
			.type(trainClass)

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
					.containment(true)
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
					.defaultValue(FALSE))
			.reference(location, referenceBuilder -> referenceBuilder
					.source(trainClass)
					.target(trackElementClass)
					.multiplicity(CardinalityIntervals.LONE, locationInvalidMultiplicity)
					.defaultValue(FALSE));

	Metamodel metamodel = metamodelBuilder.build();

	PartialRelation abstractConnectsToPreconditionPartialRelation = new PartialRelation("abstractConnectsTo_precondition", 3);
	TransitionRule abstractConnectsTo = new TransitionRule(abstractConnectsToPreconditionPartialRelation,
			Rule.of("abstractConnectsTo", (builder, trackElement1, trackElement2, trackElement3) -> builder
					.clause(
							trackElementClass.call(trackElement1),
							trackElementClass.call(trackElement2),
							trackElementClass.call(trackElement3),
							connectsTo.call(trackElement1, trackElement2),
							connectsTo.call(trackElement2, trackElement3)
					)
					.action(
							PartialActionLiterals.modify(connectsTo, UNKNOWN, trackElement1, trackElement2),
							PartialActionLiterals.modify(connectsTo, UNKNOWN, trackElement1, trackElement3)
					)));

	PartialRelation abstractSwitchPositionPreconditionPartialRelation = new PartialRelation(
			"abstractSwitchPosition_precondition",	3);
	TransitionRule abstractSwitchPosition = new TransitionRule(abstractSwitchPositionPreconditionPartialRelation,
			Rule.of("abstractSwitchPosition", (builder, swP, p1, p2) -> builder
					.clause(
							switchPositionClass.call(swP),
							positionEnum.call(p1),
							positionEnum.call(p2),
							p1.notEquivalent(p2),
							position.call(swP, p1)
					)
					.action(
							PartialActionLiterals.modify(position, UNKNOWN, swP, p1),
							PartialActionLiterals.modify(position, UNKNOWN, swP, p2)
					)));

    ModelSeed.Builder modelSeedBuilder;


	public void init(int size, MeasurementScenario scenario, Abstraction abstraction, boolean visualize) {
		this.visualize = visualize;
		this.scenario = scenario;
		this.abstraction = abstraction;
		this.size = size;

		if (visualize) {
			visualizationOutputPath = "output_visualization/trainbenchmark_" + size + "_modelSeed";
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

		classMaps.put(RegionClassName, regionClassSet);
		classMaps.put(SemaphoreClassName, semaphoreClassSet);
		classMaps.put(RouteClassName, routeClassSet);
		classMaps.put(SwitchPositionClassName, switchPositionClassSet);
		classMaps.put(SensorClassName, sensorClassSet);
		classMaps.put(SegmentClassName, segmentClassSet);
		classMaps.put(SwitchClassName, switchClassSet);
		classMaps.put(RailwayContainerClassName, railwayContainerClassSet);
		classMaps.put(TrackElementClassName, trackElementClassSet);
		classMaps.put(TrainClassName, trainClassSet);
		relationMaps.put(sensorsRelationName, sensorsMap);
		relationMaps.put(elementsRelationName, elementsMap);
		relationMaps.put(followsRelationName, followsMap);
		relationMaps.put(requiresRelationName, requiresMap);
		relationMaps.put(entryRelationName, entryMap);
		relationMaps.put(exitRelationName, exitMap);
		relationMaps.put(routeRelationName, routeMap);
		relationMaps.put(targetRelationName, targetMap);
		relationMaps.put(monitoredByRelationName, monitoredByMap);
		relationMaps.put(connectsToRelationName, connectsToMap);
		relationMaps.put(semaphoresRelationName, semaphoresMap);
		relationMaps.put(straightRelationName, straightMap);
		relationMaps.put(divergingRelationName, divergingMap);
		relationMaps.put(monitorsRelationName, monitorsMap);
		relationMaps.put(positionsRelationName, positionsMap);
		relationMaps.put(regionsRelationName, regionsMap);
		relationMaps.put(routesRelationName, routesMap);
		relationMaps.put(locationRelationName, locationMap);

		enumMaps.put(SignalEnumName, signalEnumMap);
		enumMaps.put(PositionEnumName, positionEnumMap);
		enumAttributeMaps.put(signalRelationName, signalMap);
		enumAttributeMaps.put(positionRelationName, positionMap);
		enumAttributeMaps.put(currentPositionRelationName, currentPositionMap);

		intAttributeMaps.put(lengthAttributeName, lengthMap);

		booleanAttributeMaps.put(activeAttributeName, activeMap);

		storeBuilder = storeBuilder
				.with(QueryInterpreterAdapter.builder())
				.with(PropagationAdapter.builder())
				.with(StateCoderAdapter.builder()
						.individuals(IntStream.range(0, firstFreeId).mapToObj(Tuple::of).toList())
						.stateCodeCalculatorFactory(PartialNeighborhoodCalculator.factory(Concreteness.PARTIAL, 7))
				)
				.with(ModificationAdapter.builder())
				.with(DesignSpaceExplorationAdapter.builder())
				.with(ReasoningAdapter.builder())
				.with(new MultiObjectTranslator())
				.with(new MetamodelTranslator(metamodel))
				.with(new BasePredicateTranslator(active, List.of(routeClass), Set.of(), UNKNOWN,
						new ConcretizationSettings(true, true)))
		;

		storeBuilder = storeBuilder
			.with(TransitionSystemAdapter.builder()
					.transition(abstractConnectsTo)
					.transition(abstractSwitchPosition)
					.accept(_ -> () -> false) // full state space exploration
			)
			.with(new PredicateTranslator(
					abstractConnectsToPreconditionPartialRelation,
					abstractConnectsTo.rule().getPrecondition(),
					List.of(trackElementClass, trackElementClass, trackElementClass), Set.of(), true, UNKNOWN))
			.with(new PredicateTranslator(
					abstractSwitchPositionPreconditionPartialRelation,
					abstractSwitchPosition.rule().getPrecondition(),
					List.of(switchPositionClass, positionEnum, positionEnum), Set.of(), true, UNKNOWN));
		fireOneTransitionRules.add(abstractConnectsTo.rule().getName());
		fireOneTransitionRules.add(abstractSwitchPosition.rule().getName());

		String pathToCsvFolder = "input/models/trainbenchmark3";

		signalEnumMap.put("Signal::FAILURE", Tuple.of(SIGNAL_FAILURE));
		signalEnumMap.put("Signal::STOP", Tuple.of(SIGNAL_STOP));
		signalEnumMap.put("Signal::GO", Tuple.of(SIGNAL_GO));
		positionEnumMap.put("Position::FAILURE", Tuple.of(POSITION_FAILURE));
		positionEnumMap.put("Position::STRAIGHT", Tuple.of(POSITION_STRAIGHT));
		positionEnumMap.put("Position::DIVERGING", Tuple.of(POSITION_DIVERGING));
		railwayContainerClassSet.put(Tuple.of(firstFreeId), TRUE);

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
					inputMap.put(Tuple.of(Integer.parseInt(values[0]) + firstFreeId),
							TruthValue.valueOf(values[1].toUpperCase()));
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
					inputMap.put(Tuple.of(Integer.parseInt(values[0]) + firstFreeId,
							Integer.parseInt(values[1]) + firstFreeId), TruthValue.valueOf(values[2].toUpperCase()));
				}
			} catch (java.io.IOException e) {
				e.printStackTrace();
			}
		}
		for (var csvFile :
				Objects.requireNonNull(new File(pathToCsvFolder).listFiles((dir, name) -> name.startsWith(fileNamePrefix) &&
						name.endsWith(".csv") && booleanAttributeMaps.keySet().stream().anyMatch(name::contains)))) {
			String relationName = csvFile.getName().replace(fileNamePrefix, "").replace(".csv", "");
			var inputMap = booleanAttributeMaps.get(relationName);
			try (java.io.BufferedReader br = new java.io.BufferedReader(new java.io.FileReader(csvFile))) {
				String line;
				while ((line = br.readLine()) != null) {
					String[] values = line.split(",");
					inputMap.put(Tuple.of(Integer.parseInt(values[0]) + firstFreeId), TruthValue.valueOf(values[1].toUpperCase()));
				}
			} catch (java.io.IOException e) {
				e.printStackTrace();
			}
		}
		for (var csvFile :
				Objects.requireNonNull(new File(pathToCsvFolder).listFiles((dir, name) -> name.startsWith(fileNamePrefix) &&
						name.endsWith(".csv") && intAttributeMaps.keySet().stream().anyMatch(name::contains)))) {
			String relationName = csvFile.getName().replace(fileNamePrefix, "").replace(".csv", "");
			var inputMap = intAttributeMaps.get(relationName);
			try (java.io.BufferedReader br = new java.io.BufferedReader(new java.io.FileReader(csvFile))) {
				String line;
				while ((line = br.readLine()) != null) {
					String[] values = line.split(",");
					inputMap.put(Tuple.of(Integer.parseInt(values[0]) + firstFreeId), IntInterval.of(Integer.parseInt(values[1])));
				}
			} catch (java.io.IOException e) {
				e.printStackTrace();
			}
		}
		for (var csvFile :
				Objects.requireNonNull(new File(pathToCsvFolder).listFiles((dir, name) -> name.startsWith(fileNamePrefix) &&
						name.endsWith(".csv") && enumAttributeMaps.keySet().stream().anyMatch(name::contains)))) {
			String relationName = csvFile.getName().replace(fileNamePrefix, "").replace(".csv", "");
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
					inputMap.put(Tuple.of(Integer.parseInt(values[0]) + firstFreeId, enumValue.get(0)), TruthValue.valueOf(values[2].toUpperCase()));
				}
			} catch (java.io.IOException e) {
				e.printStackTrace();
			}
		}

////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

		// TODO: This is a hack to get the number of nodes in the model. We should find a better way to do this.
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
				case RegionClassName -> regionClass;
				case SemaphoreClassName -> semaphoreClass;
				case RouteClassName -> routeClass;
				case SwitchPositionClassName -> switchPositionClass;
				case SensorClassName -> sensorClass;
				case SegmentClassName -> segmentClass;
				case SwitchClassName -> switchClass;
				case RailwayContainerClassName -> railwayContainerClass;
				case TrackElementClassName -> trackElementClass;
				case TrainClassName -> trainClass;
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
				case monitorsRelationName -> monitors;
				case sensorsRelationName -> sensors;
				case elementsRelationName -> elements;
				case positionsRelationName -> positions;
				case followsRelationName -> follows;
				case semaphoresRelationName -> semaphores;
				case regionsRelationName -> regions;
				case routesRelationName -> routes;
				case routeRelationName -> route;
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
				case requiresRelationName -> requires;
				case entryRelationName -> entry;
				case exitRelationName -> exit;
				case targetRelationName -> target;
				case monitoredByRelationName -> monitoredBy;
				case connectsToRelationName -> connectsTo;
				case straightRelationName -> straight;
				case divergingRelationName -> diverging;
				case locationRelationName -> location;
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

		for (var entry : enumMaps.entrySet()) {
			String relationName = entry.getKey();
			Map<String, Tuple> tuples = entry.getValue();
			var partialRelation = switch (relationName) {
				case SignalEnumName -> signalEnum;
				case PositionEnumName -> positionEnum;
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
			Map<Tuple, TruthValue> valueMap = mapEntry.getValue();
			var partialRelation = switch (relationName) {
				case signalRelationName -> signal;
				case positionRelationName -> position;
				case currentPositionRelationName -> currentPosition;
				default -> throw new IllegalArgumentException("Unknown relation name: " + relationName);
			};
			modelSeedBuilder.seed(partialRelation, builder -> {
				for (var valueEntry : valueMap.entrySet()) {
					builder.put(valueEntry.getKey(), valueEntry.getValue());
				}
				builder.reducedValue(FALSE);
			});
		}

		for (var mapEntry : booleanAttributeMaps.entrySet()) {
			String relationName = mapEntry.getKey();
			Map<Tuple, TruthValue> valueMap = mapEntry.getValue();
			if (!relationName.equals(activeAttributeName)) {
				throw new IllegalArgumentException("Unknown relation name: " + relationName);
			}
			var partialRelation = active;
			modelSeedBuilder.seed(partialRelation, builder -> {
				for (var valueEntry : valueMap.entrySet()) {
					builder.put(valueEntry.getKey(), valueEntry.getValue());
				}
				builder.reducedValue(UNKNOWN);
			});
		}

		for (var mapEntry : intAttributeMaps.entrySet()) {
			String relationName = mapEntry.getKey();
			Map<Tuple, IntInterval> valueMap = mapEntry.getValue();
			if (!relationName.equals(lengthAttributeName)) {
				throw new IllegalArgumentException("Unknown relation name: " + relationName);
			}
			var partialRelation = length;
			modelSeedBuilder.seed(partialRelation, builder -> {
				for (var valueEntry : valueMap.entrySet()) {
					builder.put(valueEntry.getKey(), valueEntry.getValue());
				}
				builder.reducedValue(IntInterval.UNKNOWN);
			});
		}

		modelSeedBuilder.seed(abstractConnectsToPreconditionPartialRelation, builder -> builder.reducedValue(UNKNOWN));
		modelSeedBuilder.seed(abstractSwitchPositionPreconditionPartialRelation, builder -> builder.reducedValue(UNKNOWN));

		this.modelSeed = this.modelSeedBuilder.build();
		super.init();
	}

	public void saveModelToFile(String filename) {
		var reasoningAdapter = model.getAdapter(ReasoningAdapter.class);
		reasoningAdapter.getPartialSymbols().forEach(symbol -> {
//			If the symbol is contained in a list of symbols to ignore, skip it
			if (!List.of(active, connectsTo, currentPosition, elements, entry, exit, follows, length, monitoredBy,
					position, regionClass, requires, routeClass, segmentClass, semaphoreClass, semaphores,
					sensorClass, sensors, signal,switchClass, switchPositionClass, target).contains(symbol)) {
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

