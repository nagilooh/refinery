package tools.refinery.measurement;

import tools.refinery.logic.dnf.Query;
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
import tools.refinery.store.query.interpreter.QueryInterpreterAdapter;
import tools.refinery.store.reasoning.ReasoningAdapter;
import tools.refinery.store.reasoning.actions.*;
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
import static tools.refinery.store.reasoning.literal.PartialLiterals.partialCheck;

public class TrainBenchmarkMeasurement extends MeasurementBase {

	String fileNamePrefix = "railway-batch-";

	int firstFreeId = 0;
	int POSITION_FAILURE = firstFreeId++;
	int POSITION_STRAIGHT = firstFreeId++;
	int POSITION_DIVERGING = firstFreeId++;
	int SIGNAL_FAILURE = firstFreeId++;
	int SIGNAL_STOP = firstFreeId++;
	int SIGNAL_GO = firstFreeId++;

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


	PartialRelation signalInvalidMultiplicity = new PartialRelation("signal::invalidMultiplicity", 1);
	PartialRelation requiresInvalidMultiplicity = new PartialRelation("requires::invalidMultiplicity", 1);
	PartialRelation entryInvalidMultiplicity = new PartialRelation("entry::invalidMultiplicity", 1);
	PartialRelation exitInvalidMultiplicity = new PartialRelation("exit::invalidMultiplicity", 1);
	PartialRelation targetInvalidMultiplicity = new PartialRelation("target::invalidMultiplicity", 1);
	PartialRelation positionInvalidMultiplicity = new PartialRelation("position::invalidMultiplicity", 1);
	PartialRelation currentPositionInvalidMultiplicity = new PartialRelation("currentPosition::invalidMultiplicity", 1);
	PartialRelation straightInvalidMultiplicity = new PartialRelation("straight::invalidMultiplicity", 1);
	PartialRelation divergingInvalidMultiplicity = new PartialRelation("diverging::invalidMultiplicity", 1);


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
					.defaultValue(FALSE));

	Metamodel metamodel = metamodelBuilder.build();

	PartialRelation injectConnectedSegmentsPreconditionPartialRelation = new PartialRelation("injectConnectedSegments_precondition", 3);
	TransitionRule injectConnectedSegments = new TransitionRule(injectConnectedSegmentsPreconditionPartialRelation,
			Rule.of("injectConnectedSegments", (builder, sensor, segment1,  segment3) -> builder
					.clause(
							sensorClass.call(sensor),
							segmentClass.call(segment1),
							segmentClass.call(segment3),
							monitoredBy.call(segment1, sensor),
							monitoredBy.call(segment3, sensor),
							connectsTo.call(segment1, segment3),
							segment1.notEquivalent(segment3)
					)
					.action(segment2 -> List.of(
							new CreateNewActionLiteral(segment2),
							PartialActionLiterals.modify(length, IntInterval.of(500), segment2),
							PartialActionLiterals.modify(segmentClass, TRUE, segment2),
							PartialActionLiterals.modify(connectsTo, FALSE, segment1, segment3),
							PartialActionLiterals.modify(connectsTo, TRUE, segment1, segment2),
							PartialActionLiterals.modify(connectsTo, TRUE, segment2, segment3),
							PartialActionLiterals.modify(monitoredBy, TRUE, segment1, sensor))
					)));

	PartialRelation repairConnectedSegmentsPreconditionPartialRelation = new PartialRelation(
			"repairConnectedSegments_precondition", 3);
	TransitionRule repairConnectedSegments = new TransitionRule(repairConnectedSegmentsPreconditionPartialRelation,
			Rule.of("repairConnectedSegments", (builder, segment1, segment2, segment3) -> builder
					.clause( (segment4, segment5, segment6, sensor) -> List.of(
							segmentClass.call(segment1),
							segmentClass.call(segment2),
							segmentClass.call(segment3),
							segmentClass.call(segment4),
							segmentClass.call(segment5),
							segmentClass.call(segment6),
							connectsTo.call(segment1, segment2),
							connectsTo.call(segment2, segment3),
							connectsTo.call(segment3, segment4),
							connectsTo.call(segment4, segment5),
							connectsTo.call(segment5, segment6),
							monitoredBy.call(segment1, sensor),
							monitoredBy.call(segment2, sensor),
							monitoredBy.call(segment3, sensor),
							monitoredBy.call(segment4, sensor),
							monitoredBy.call(segment5, sensor),
							monitoredBy.call(segment6, sensor),
							segment1.notEquivalent(segment2),
							segment1.notEquivalent(segment3),
							segment1.notEquivalent(segment4),
							segment1.notEquivalent(segment5),
							segment1.notEquivalent(segment6),
							segment2.notEquivalent(segment3),
							segment2.notEquivalent(segment4),
							segment2.notEquivalent(segment5),
							segment2.notEquivalent(segment6),
							segment3.notEquivalent(segment4),
							segment3.notEquivalent(segment5),
							segment3.notEquivalent(segment6),
							segment4.notEquivalent(segment5),
							segment4.notEquivalent(segment6),
							segment5.notEquivalent(segment6)
						)
					)
					.action(
							new CleanupActionLiteral(segment2),
							PartialActionLiterals.modify(connectsTo, TRUE, segment1, segment3)
					)));

	PartialRelation injectPosLengthPreconditionPartialRelation = new PartialRelation("injectPosLength_precondition", 1);
	TransitionRule injectPosLength = new TransitionRule(injectPosLengthPreconditionPartialRelation,
			Rule.of("injectPosLength", (builder, segment) -> builder
					.clause(
							segmentClass.call(segment)
					)
					.action(
							PartialActionLiterals.modify(length, IntInterval.of(0), segment)
					)));

	PartialRelation repairPosLengthPreconditionPartialRelation = new PartialRelation("repairPosLength_precondition", 1);
	TransitionRule repairPosLength = new TransitionRule(repairPosLengthPreconditionPartialRelation,
			Rule.of("repairPosLength", (builder, segment) -> builder
					.clause(
							segmentClass.call(segment),
							partialCheck(AbstractDomainTerms.lessEq(
									IntIntervalDomain.INSTANCE,
									length.call(segment),
									IntIntervalTerms.constant(IntInterval.ZERO)
							))
					)
					.action(
							PartialActionLiterals.modifyComputed(
									length,
									List.of(segment),
									Query.of(IntInterval.class, (builder2, segment2, output) -> builder2.clause(
											segmentClass.call(segment2),
											output.assign(IntIntervalTerms.add(
													IntIntervalTerms.mul(
															IntIntervalTerms.constant(IntInterval.of(-1)),
															length.call(segment2)
													),
													IntIntervalTerms.constant(IntInterval.ONE)
											)))),
									List.of(segment)
							)
					)));

	PartialRelation injectRouteSensorPreconditionPartialRelation = new PartialRelation(
			"injectRouteSensor_precondition", 2);
	TransitionRule injectRouteSensor = new TransitionRule(injectRouteSensorPreconditionPartialRelation,
			Rule.of("injectRouteSensor", (builder, route, sensor) -> builder
					.clause(
							routeClass.call(route),
							sensorClass.call(sensor),
							requires.call(route, sensor)
					)
					.action(
							PartialActionLiterals.modify(requires, FALSE, route, sensor)
					)));

	PartialRelation repairRouteSensorPreconditionPartialRelation = new PartialRelation(
			"repairRouteSensor_precondition", 2);
	TransitionRule repairRouteSensor = new TransitionRule(repairRouteSensorPreconditionPartialRelation,
			Rule.of("repairRouteSensor", (builder, route, sensor) -> builder
					.clause((sw, swP) -> List.of(
							routeClass.call(route),
							sensorClass.call(sensor),
							monitoredBy.call(sw, sensor),
							target.call(swP, sw),
							follows.call(route, swP),
							not(requires.call(route, sensor))
						)
					)
					.action(
							PartialActionLiterals.modify(requires, TRUE, route, sensor)
					)));

	PartialRelation injectSemaphoreNeighborPreconditionPartialRelation = new PartialRelation(
			"injectSemaphoreNeighbor_precondition", 2);
	TransitionRule injectSemaphoreNeighbor = new TransitionRule(injectSemaphoreNeighborPreconditionPartialRelation,
			Rule.of("injectSemaphoreNeighbor", (builder, semaphore, route) -> builder
					.clause(
							semaphoreClass.call(semaphore),
							routeClass.call(route),
							entry.call(route, semaphore)
					)
					.action(
							PartialActionLiterals.modify(entry, FALSE, route, semaphore)
					)));

	PartialRelation repairSemaphoreNeighborPreconditionPartialRelation = new PartialRelation(
			"repairSemaphoreNeighbor_precondition", 2);
	TransitionRule repairSemaphoreNeighbor = new TransitionRule(repairSemaphoreNeighborPreconditionPartialRelation,
			Rule.of("repairSemaphoreNeighbor", (builder, semaphore, route2) -> builder
					.clause((route1, sensor1, sensor2, te1, te2) -> List.of(
							exit.call(route1, semaphore),
							requires.call(route1, sensor1),
							monitoredBy.call(te1, sensor1),
							connectsTo.call(te1, te2),
							monitoredBy.call(te2, sensor2),
							requires.call(route2, sensor2),
							not(entry.call(route2, semaphore)),
							route1.notEquivalent(route2)
						)
					)
					.action(
							PartialActionLiterals.modify(entry, TRUE, route2, semaphore)
					)));

	PartialRelation injectSwitchMonitoredPreconditionPartialRelation = new PartialRelation(
			"injectSwitchMonitored_precondition", 2);
	TransitionRule injectSwitchMonitored = new TransitionRule(injectSwitchMonitoredPreconditionPartialRelation,
			Rule.of("injectRouteSensor", (builder, sw, sensor) -> builder
					.clause(
							switchClass.call(sw),
							sensorClass.call(sensor),
							monitoredBy.call(sw, sensor)
					)
					.action(
							PartialActionLiterals.modify(monitoredBy, FALSE, sw, sensor)
					)));

	PartialRelation repairSwitchMonitoredPreconditionPartialRelation = new PartialRelation(
			"repairSwitchMonitored_precondition", 1);
	TransitionRule repairSwitchMonitored = new TransitionRule(repairSwitchMonitoredPreconditionPartialRelation,
			Rule.of("repairRouteSensor", (builder, sw) -> builder
					.clause(
							switchClass.call(sw),
							not(monitoredBy.call(sw, Variable.of()))
					)
					.action(sensor -> List.of(
							new CreateNewActionLiteral(sensor),
							PartialActionLiterals.modify(sensorClass, TRUE, sensor),
							PartialActionLiterals.modify(monitoredBy, TRUE, sw, sensor)
						)
					)));

	PartialRelation injectSwitchSetPreconditionPartialRelation = new PartialRelation(
			"injectSwitchSet_precondition", 3);
	TransitionRule injectSwitchSet = new TransitionRule(injectSwitchSetPreconditionPartialRelation,
			Rule.of("injectSwitchSet", (builder, sw, currentPos, pos) -> builder
					.clause(
							switchClass.call(sw),
							positionEnum.call(currentPos),
							positionEnum.call(pos),
							currentPosition.call(sw, currentPos),
							currentPos.notEquivalent(pos)
					)
					.action(
							PartialActionLiterals.modify(currentPosition, FALSE, sw, currentPos),
							PartialActionLiterals.modify(currentPosition, TRUE, sw, pos)
					)));

	PartialRelation repairSwitchSetPreconditionPartialRelation = new PartialRelation(
			"repairSwitchSet_precondition", 3);
	TransitionRule repairSwitchSet = new TransitionRule(repairSwitchSetPreconditionPartialRelation,
			Rule.of("repairSwitchSet", (builder, sw, swCurrentPosition, swpPosition) -> builder
					.clause((route, semaphore, swP, GO) -> List.of(
							switchClass.call(sw),
							positionEnum.call(swCurrentPosition),
							positionEnum.call(swpPosition),
							GO.isConstant(this.SIGNAL_GO),

							active.call(route),
							entry.call(route, semaphore),
							follows.call(route, swP),
							target.call(swP, sw),

							signal.call(semaphore, GO),
							position.call(swP, swpPosition),
							currentPosition.call(sw, swCurrentPosition),
							swCurrentPosition.notEquivalent(swpPosition)
						)
					)
					.action(
							PartialActionLiterals.modify(currentPosition, FALSE, sw, swCurrentPosition),
							PartialActionLiterals.modify(currentPosition, TRUE, sw, swpPosition)
					)));

    ModelSeed.Builder modelSeedBuilder;

	public void init(int size, MeasurementScenario scenario, boolean visualize) {
		this.visualize = visualize;
		if (visualize) {
			visualizationOutputPath = "output_visualization/trainbenchmark_" + size + "_modelSeed";
		}

		fileNamePrefix = fileNamePrefix + size;

		classMaps.put(RegionClassName, regionClassSet);
		classMaps.put(SemaphoreClassName, semaphoreClassSet);
		classMaps.put(RouteClassName, routeClassSet);
		classMaps.put(SwitchPositionClassName, switchPositionClassSet);
		classMaps.put(SensorClassName, sensorClassSet);
		classMaps.put(SegmentClassName, segmentClassSet);
		classMaps.put(SwitchClassName, switchClassSet);
		classMaps.put(RailwayContainerClassName, railwayContainerClassSet);
		classMaps.put(TrackElementClassName, trackElementClassSet);
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

		enumMaps.put(SignalEnumName, signalEnumMap);
		enumMaps.put(PositionEnumName, positionEnumMap);
		enumAttributeMaps.put(signalRelationName, signalMap);
		enumAttributeMaps.put(positionRelationName, positionMap);
		enumAttributeMaps.put(currentPositionRelationName, currentPositionMap);

		intAttributeMaps.put(lengthAttributeName, lengthMap);

		booleanAttributeMaps.put(activeAttributeName, activeMap);

		storeBuilder = storeBuilder
				.with(QueryInterpreterAdapter.builder()
						.queries())
				.with(PropagationAdapter.builder())
				.with(StateCoderAdapter.builder()
						.individuals(IntStream.range(0, firstFreeId).mapToObj(Tuple::of).toList())
						.stateCodeCalculatorFactory(PartialNeighborhoodCalculator.factory(Concreteness.PARTIAL, 7))
				)
				.with(ModificationAdapter.builder())
				.with(DesignSpaceExplorationAdapter.builder())
				.with(ReasoningAdapter.builder())
				.with(TransitionSystemAdapter.builder()
						.transition(injectConnectedSegments)
						.transition(repairConnectedSegments)
						.transition(injectPosLength)
						.transition(repairPosLength)
						.transition(injectRouteSensor)
						.transition(repairRouteSensor)
						.transition(injectSemaphoreNeighbor)
						.transition(repairSemaphoreNeighbor)
						.transition(injectSwitchMonitored)
						.transition(repairSwitchMonitored)
						.transition(injectSwitchSet)
						.transition(repairSwitchSet)
						.accept(_ -> () -> false) // full state space exploration
				)
				.with(new MultiObjectTranslator())
				.with(new MetamodelTranslator(metamodel))
				.with(new BasePredicateTranslator(active, List.of(routeClass), Set.of(), UNKNOWN,
						new ConcretizationSettings(true, true)))
				.with(new PredicateTranslator(
						injectConnectedSegmentsPreconditionPartialRelation,
						injectConnectedSegments.rule().getPrecondition(),
						List.of(sensorClass, segmentClass, segmentClass), Set.of(), true, UNKNOWN))
				.with(new PredicateTranslator(
						repairConnectedSegmentsPreconditionPartialRelation,
						repairConnectedSegments.rule().getPrecondition(),
						List.of(segmentClass, segmentClass, segmentClass), Set.of(), true, UNKNOWN))
				.with(new PredicateTranslator(
						injectPosLengthPreconditionPartialRelation,
						injectPosLength.rule().getPrecondition(),
						List.of(segmentClass), Set.of(), true, UNKNOWN))
				.with(new PredicateTranslator(
						repairPosLengthPreconditionPartialRelation,
						repairPosLength.rule().getPrecondition(),
						List.of(segmentClass), Set.of(), true, UNKNOWN))
				.with(new PredicateTranslator(
						injectRouteSensorPreconditionPartialRelation,
						injectRouteSensor.rule().getPrecondition(),
						List.of(routeClass, sensorClass), Set.of(), true, UNKNOWN))
				.with(new PredicateTranslator(
						repairRouteSensorPreconditionPartialRelation,
						repairRouteSensor.rule().getPrecondition(),
						List.of(routeClass, sensorClass), Set.of(), true, UNKNOWN))
				.with(new PredicateTranslator(
						injectSemaphoreNeighborPreconditionPartialRelation,
						injectSemaphoreNeighbor.rule().getPrecondition(),
						List.of(semaphoreClass, routeClass), Set.of(), true, UNKNOWN))
				.with(new PredicateTranslator(
						repairSemaphoreNeighborPreconditionPartialRelation,
						repairSemaphoreNeighbor.rule().getPrecondition(),
						List.of(semaphoreClass, routeClass), Set.of(), true, UNKNOWN))
				.with(new PredicateTranslator(
						injectSwitchMonitoredPreconditionPartialRelation,
						injectSwitchMonitored.rule().getPrecondition(),
						List.of(switchClass, sensorClass), Set.of(), true, UNKNOWN))
				.with(new PredicateTranslator(
						repairSwitchMonitoredPreconditionPartialRelation,
						repairSwitchMonitored.rule().getPrecondition(),
						List.of(switchClass), Set.of(), true, UNKNOWN))
				.with(new PredicateTranslator(
						injectSwitchSetPreconditionPartialRelation,
						injectSwitchSet.rule().getPrecondition(),
						List.of(switchClass, positionEnum, positionEnum), Set.of(), true, UNKNOWN))
				.with(new PredicateTranslator(
						repairSwitchSetPreconditionPartialRelation,
						repairSwitchSet.rule().getPrecondition(),
						List.of(switchClass, positionEnum, positionEnum), Set.of(), true, UNKNOWN))
		;

		String pathToCsvFolder = "input/models/trainbenchmark/size-" + size;

		signalEnumMap.put("Signal::FAILURE", Tuple.of(SIGNAL_FAILURE));
		signalEnumMap.put("Signal::STOP", Tuple.of(SIGNAL_STOP));
		signalEnumMap.put("Signal::GO", Tuple.of(SIGNAL_GO));
		positionEnumMap.put("Position::FAILURE", Tuple.of(POSITION_FAILURE));
		positionEnumMap.put("Position::STRAIGHT", Tuple.of(POSITION_STRAIGHT));
		positionEnumMap.put("Position::DIVERGING", Tuple.of(POSITION_DIVERGING));
		railwayContainerClassSet.add(Tuple.of(firstFreeId));

		for (var csvFile :
				Objects.requireNonNull(new File(pathToCsvFolder).listFiles((dir, name) -> name.startsWith(fileNamePrefix) &&
						name.endsWith(".csv") && classMaps.keySet().stream().anyMatch(name::contains)))) {
			String relationName = csvFile.getName().replace(fileNamePrefix + "-", "").replace(".csv", "");
			var inputMap = classMaps.get(relationName);
			try (java.io.BufferedReader br = new java.io.BufferedReader(new java.io.FileReader(csvFile))) {
				String line;
				while ((line = br.readLine()) != null) {
					String[] values = line.split(",");
					inputMap.add(Tuple.of(Integer.parseInt(values[0]) + firstFreeId));
				}
			} catch (java.io.IOException e) {
				e.printStackTrace();
			}
		}
		for (var csvFile :
				Objects.requireNonNull(new File(pathToCsvFolder).listFiles((dir, name) -> name.startsWith(fileNamePrefix) &&
						name.endsWith(".csv") && relationMaps.keySet().stream().anyMatch(name::contains)))) {
			String relationName = csvFile.getName().replace(fileNamePrefix + "-", "").replace(".csv", "");
			var inputMap = relationMaps.get(relationName);
			try (java.io.BufferedReader br = new java.io.BufferedReader(new java.io.FileReader(csvFile))) {
				String line;
				while ((line = br.readLine()) != null) {
					String[] values = line.split(",");
					inputMap.add(Tuple.of(Integer.parseInt(values[0]) + firstFreeId, Integer.parseInt(values[1]) + firstFreeId));
				}
			} catch (java.io.IOException e) {
				e.printStackTrace();
			}
		}
		for (var csvFile :
				Objects.requireNonNull(new File(pathToCsvFolder).listFiles((dir, name) -> name.startsWith(fileNamePrefix) &&
						name.endsWith(".csv") && booleanAttributeMaps.keySet().stream().anyMatch(name::contains)))) {
			String relationName = csvFile.getName().replace(fileNamePrefix + "-", "").replace(".csv", "");
			var inputMap = booleanAttributeMaps.get(relationName);
			try (java.io.BufferedReader br = new java.io.BufferedReader(new java.io.FileReader(csvFile))) {
				String line;
				while ((line = br.readLine()) != null) {
					String[] values = line.split(",");
					inputMap.put(Tuple.of(Integer.parseInt(values[0]) + firstFreeId), of(Boolean.parseBoolean(values[1])));
				}
			} catch (java.io.IOException e) {
				e.printStackTrace();
			}
		}
		for (var csvFile :
				Objects.requireNonNull(new File(pathToCsvFolder).listFiles((dir, name) -> name.startsWith(fileNamePrefix) &&
						name.endsWith(".csv") && intAttributeMaps.keySet().stream().anyMatch(name::contains)))) {
			String relationName = csvFile.getName().replace(fileNamePrefix + "-", "").replace(".csv", "");
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
			String relationName = csvFile.getName().replace(fileNamePrefix + "-", "").replace(".csv", "");
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
					inputMap.add(Tuple.of(Integer.parseInt(values[0]) + firstFreeId, enumValue.get(0)));
				}
			} catch (java.io.IOException e) {
				e.printStackTrace();
			}
		}

////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

		// TODO: This is a hack to get the number of nodes in the model. We should find a better way to do this.
		var nodeCount = classMaps.values().stream().flatMap(Set::stream).mapToInt(t -> t.get(0)).max().orElse(0) + 1;
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
				case RegionClassName -> regionClass;
				case SemaphoreClassName -> semaphoreClass;
				case RouteClassName -> routeClass;
				case SwitchPositionClassName -> switchPositionClass;
				case SensorClassName -> sensorClass;
				case SegmentClassName -> segmentClass;
				case SwitchClassName -> switchClass;
				case RailwayContainerClassName -> railwayContainerClass;
				case TrackElementClassName -> trackElementClass;
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
				case requiresRelationName -> requires;
				case entryRelationName -> entry;
				case exitRelationName -> exit;
				case targetRelationName -> target;
				case monitoredByRelationName -> monitoredBy;
				case connectsToRelationName -> connectsTo;
				case straightRelationName -> straight;
				case divergingRelationName -> diverging;
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
			Set<Tuple> tuples = mapEntry.getValue();
			var partialRelation = switch (relationName) {
				case signalRelationName -> signal;
				case positionRelationName -> position;
				case currentPositionRelationName -> currentPosition;
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
			if (!relationName.equals(activeAttributeName)) {
				throw new IllegalArgumentException("Unknown relation name: " + relationName);
			}
			var partialRelation = active;
			modelSeedBuilder.seed(partialRelation, builder -> {
				for (var tupleEntry : tuples.entrySet()) {
					builder.put(tupleEntry.getKey(), tupleEntry.getValue());
				}
				builder.reducedValue(UNKNOWN);
			});
		}

		for (var mapEntry : intAttributeMaps.entrySet()) {
			String relationName = mapEntry.getKey();
			Map<Tuple, IntInterval> tuples = mapEntry.getValue();
			if (!relationName.equals(lengthAttributeName)) {
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

		modelSeedBuilder.seed(injectConnectedSegmentsPreconditionPartialRelation, builder -> builder.reducedValue(UNKNOWN));
		modelSeedBuilder.seed(repairConnectedSegmentsPreconditionPartialRelation, builder -> builder.reducedValue(UNKNOWN));
		modelSeedBuilder.seed(injectPosLengthPreconditionPartialRelation, builder -> builder.reducedValue(UNKNOWN));
		modelSeedBuilder.seed(repairPosLengthPreconditionPartialRelation, builder -> builder.reducedValue(UNKNOWN));
		modelSeedBuilder.seed(injectRouteSensorPreconditionPartialRelation, builder -> builder.reducedValue(UNKNOWN));
		modelSeedBuilder.seed(repairRouteSensorPreconditionPartialRelation, builder -> builder.reducedValue(UNKNOWN));
		modelSeedBuilder.seed(injectSemaphoreNeighborPreconditionPartialRelation, builder -> builder.reducedValue(UNKNOWN));
		modelSeedBuilder.seed(repairSemaphoreNeighborPreconditionPartialRelation, builder -> builder.reducedValue(UNKNOWN));
		modelSeedBuilder.seed(injectSwitchMonitoredPreconditionPartialRelation, builder -> builder.reducedValue(UNKNOWN));
		modelSeedBuilder.seed(repairSwitchMonitoredPreconditionPartialRelation, builder -> builder.reducedValue(UNKNOWN));
		modelSeedBuilder.seed(injectSwitchSetPreconditionPartialRelation, builder -> builder.reducedValue(UNKNOWN));
		modelSeedBuilder.seed(repairSwitchSetPreconditionPartialRelation, builder -> builder.reducedValue(UNKNOWN));

        this.modelSeed = this.modelSeedBuilder.build();
		super.init();
    }

	@Override
	protected boolean renderTransitionsToAlreadyVisitedStates() {
		return true;
	}
}

