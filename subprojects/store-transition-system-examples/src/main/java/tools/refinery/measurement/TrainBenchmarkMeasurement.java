package tools.refinery.measurement;

import sun.misc.Signal;
import tools.refinery.logic.dnf.Query;
import tools.refinery.logic.dnf.QueryBuilder;
import tools.refinery.logic.dnf.RelationalQuery;
import tools.refinery.logic.literal.EquivalenceLiteral;
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
import tools.refinery.store.query.ModelQueryAdapter;
import tools.refinery.store.query.ModelQueryBuilder;
import tools.refinery.store.query.interpreter.QueryInterpreterAdapter;
import tools.refinery.store.reasoning.ReasoningAdapter;
import tools.refinery.store.reasoning.ReasoningBuilder;
import tools.refinery.store.reasoning.actions.*;
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
import static tools.refinery.store.reasoning.literal.PartialLiterals.*;

public class TrainBenchmarkMeasurement extends MeasurementBase {

	String fileNamePrefix = "railway-";

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
	Set<Tuple> trainClassSet = new HashSet<>();

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
	Set<Tuple> locationMap = new HashSet<>();
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

	PartialRelation posLengthPartialRelation = new PartialRelation("posLength", 1);
	RelationalQuery posLength;
	RelationalQuery posLengthLifted;
	PartialRelation switchMonitoredPartialRelation = new PartialRelation("switchMonitored", 1);
	RelationalQuery switchMonitored;
	RelationalQuery switchMonitoredLifted;
	PartialRelation routeSensorPartialRelation = new PartialRelation("routeSensor", 4);
	RelationalQuery routeSensor;
	RelationalQuery routeSensorLifted;
	PartialRelation switchSetPartialRelation = new PartialRelation("switchSet", 1);
	RelationalQuery switchSet;
	RelationalQuery switchSetLifted;
	PartialRelation connectedSegmentsPartialRelation = new PartialRelation("connectedSegments", 6);
	RelationalQuery connectedSegments;
	RelationalQuery connectedSegmentsLifted;
	PartialRelation semaphoreNeighborPartialRelation = new PartialRelation("semaphoreNeighbor", 2);
	RelationalQuery semaphoreNeighbor;
	RelationalQuery semaphoreNeighborLifted;

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
											must(segmentClass.call(segment2)),
											output.assign(IntIntervalTerms.add(
													IntIntervalTerms.mul(
															IntIntervalTerms.constant(IntInterval.of(-1)),
															length.call(Concreteness.PARTIAL, segment2)
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
			Rule.of("injectSwitchMonitored", (builder, sw, sensor) -> builder
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
			Rule.of("repairSwitchMonitored", (builder, sw) -> builder
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

	RelationalQuery routeExit = Query.of("routeExit", (builder, route, segment) -> builder
			.clause(semaphore -> List.of(
					routeClass.call(route),
					segmentClass.call(segment),
					exit.call(route, semaphore),
					semaphores.call(segment, semaphore)
			)));

	RelationalQuery activeRouteExit = Query.of("activeRouteExit", (builder, route, segment) -> builder
			.clause(
					active.call(route),
					routeExit.call(route, segment)
			));

	RelationalQuery routeEntry = Query.of("routeEntry", (builder, route, segment) -> builder
			.clause(semaphore -> List.of(
					routeClass.call(route),
					segmentClass.call(segment),
					entry.call(route, semaphore),
					semaphores.call(segment, semaphore)
			)));

	RelationalQuery activeRouteEntry = Query.of("activeRouteEntry", (builder, route, segment) -> builder
			.clause(
					active.call(route),
					routeEntry.call(route, segment)
			));

	RelationalQuery activeRouteSwitch = Query.of("activeRouteSwitch", (builder, route, sw) -> builder
			.clause(swP -> List.of(
					routeClass.call(route),
					switchClass.call(sw),
					active.call(route),
					follows.call(route, swP),
					target.call(swP, sw)
			)));

	PartialRelation activateRoutePreconditionPartialRelation = new PartialRelation("activateRoute_precondition", 1);
	TransitionRule activateRoute = new TransitionRule(activateRoutePreconditionPartialRelation,
			Rule.of("activateRoute", (builder, route) -> builder
					.clause(segment -> List.of(
							routeClass.call(route),
							not(active.call(route)),
							routeEntry.call(route, segment),
							not(activeRouteEntry.call(Variable.of(), segment))
					))
					.action(
							PartialActionLiterals.modify(active, TRUE, route)
					)));

	PartialRelation placeTrainOnRoutePreconditionPartialRelation = new PartialRelation(
			"placeTrainOnRoute_precondition", 3);
	TransitionRule placeTrainOnRoute = new TransitionRule(placeTrainOnRoutePreconditionPartialRelation,
			Rule.of("placeTrainOnRoute", (builder, entrySegment, semaphore, STOP) -> builder
					.clause((route, GO) -> List.of(
							GO.isConstant(this.SIGNAL_GO),
							STOP.isConstant(this.SIGNAL_STOP),
							segmentClass.call(entrySegment),
							semaphoreClass.call(semaphore),
							active.call(route),
							semaphores.call(entrySegment, semaphore),
							entry.call(route, semaphore),
							signal.call(semaphore, GO)
					))
					.action((train) -> List.of(
							new CreateNewActionLiteral(train),
							PartialActionLiterals.modify(location, TRUE, train, entrySegment),
							PartialActionLiterals.modify(signal, TRUE, semaphore, STOP)
					))));

	PartialRelation changeSwitchPreconditionPartialRelation = new PartialRelation("changeSwitch_precondition", 3);
	TransitionRule changeSwitch = new TransitionRule(changeSwitchPreconditionPartialRelation,
			Rule.of("changeSwitch", (builder, sw, currentPos, pos) -> builder
					.clause(
							switchClass.call(sw),
							positionEnum.call(currentPos),
							positionEnum.call(pos),
							not(activeRouteSwitch.call(Variable.of(), sw)),
							currentPosition.call(sw, currentPos),
							currentPos.notEquivalent(pos)
					)
					.action(
							PartialActionLiterals.modify(currentPosition, FALSE, sw, currentPos),
							PartialActionLiterals.modify(currentPosition, TRUE, sw, pos)
					)));

	PartialRelation moveTrainOnRoutePreconditionPartialRelation = new PartialRelation("moveTrainOnRoute_precondition"
			, 3);
	TransitionRule moveTrainOnRoute = new TransitionRule(moveTrainOnRoutePreconditionPartialRelation,
			Rule.of("moveTrainOnRoute", (builder, train, from, to) -> builder
					.clause(
							trainClass.call(train),
							trackElementClass.call(from),
							trackElementClass.call(to),
							location.call(train, from),
							not(activeRouteExit.call(Variable.of(), to))
					)
					.action(
							PartialActionLiterals.modify(location, FALSE, train, from),
							PartialActionLiterals.modify(location, TRUE, train, to)
					)));

	PartialRelation activateNextRoutePreconditionPartialRelation = new PartialRelation("activateNextRoute_precondition", 1);
	TransitionRule activateNextRoute = new TransitionRule(activateNextRoutePreconditionPartialRelation,
			Rule.of("activateNextRoute", (builder, newRoute) -> builder
					.clause((oldRoute, from, to, semaphore, GO) -> List.of(
							GO.isConstant(this.SIGNAL_GO),
							routeClass.call(oldRoute),
							active.call(oldRoute),
							not(active.call(newRoute)),
							connectsTo.call(from, to),
							location.call(Variable.of(), from),
							exit.call(oldRoute, semaphore),
							semaphores.call(to, semaphore),
							routeEntry.call(newRoute, to),
							signal.call(semaphore, GO)
					))
					.action(
							PartialActionLiterals.modify(active, TRUE, newRoute)
					)));

	PartialRelation moveTrainToNextRoutePreconditionPartialRelation;
	TransitionRule moveTrainToNextRoute;

    ModelSeed.Builder modelSeedBuilder;

	Criterion acceptanceCriterion = _ -> () -> false;

	public void init(int size, MeasurementScenario scenario, boolean visualize) {
		this.visualize = visualize;
		if (visualize) {
			visualizationOutputPath = "output_visualization/trainbenchmark_" + size + "_modelSeed";
		}

		fileNamePrefix = fileNamePrefix + scenario.toString().toLowerCase() + "-" + size + "-";
		System.out.println(fileNamePrefix);

		posLength = Query.of("posLength", (builder, segment) -> builder
				.clause(
						segmentClass.call(segment),
						partialCheck(AbstractDomainTerms.lessEq(
								IntIntervalDomain.INSTANCE,
								length.call(Concreteness.PARTIAL, segment),
								IntIntervalTerms.constant(IntInterval.ZERO)
						))
				));

		switchMonitored = Query.of("switchMonitored", (builder, sw) -> builder
				.clause(
						switchClass.call(sw),
						not(monitoredBy.call(sw, Variable.of()))
				));

		routeSensor = Query.of("routeSensor", (builder, route, sensor, swP, sw) -> builder
				.clause(
						routeClass.call(route),
						sensorClass.call(sensor),
						monitoredBy.call(sw, sensor),
						target.call(swP, sw),
						follows.call(route, swP),
						not(requires.call(route, sensor)
						)));

		switchSet = Query.of("switchSet", (builder, semaphore, route, swP, sw) -> builder
				.clause((GO, swCurrentPosition, swpPosition) -> List.of(
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
				)));

		semaphoreNeighbor = Query.of("semaphoreNeighbor", (builder, route1, route2) -> builder
				.clause((semaphore, sensor1, sensor2, te1, te2) -> List.of(
						exit.call(route1, semaphore),
						requires.call(route1, sensor1),
						monitoredBy.call(te1, sensor1),
						connectsTo.call(te1, te2),
						monitoredBy.call(te2, sensor2),
						requires.call(route2, sensor2),
						not(entry.call(route2, semaphore)),
						route1.notEquivalent(route2)
				)));

		QueryBuilder queryBuilder = Query.builder("connectedSegments");
		NodeVariable parameterSensor = queryBuilder.parameter("sensor");
		NodeVariable parameterSegment1 = queryBuilder.parameter("segment1");
		NodeVariable parameterSegment2 = queryBuilder.parameter("segment2");
		NodeVariable parameterSegment3 = queryBuilder.parameter("segment3");
		NodeVariable parameterSegment4 = queryBuilder.parameter("segment4");
		NodeVariable parameterSegment5 = queryBuilder.parameter("segment5");
		NodeVariable parameterSegment6 = queryBuilder.parameter("segment6");
		queryBuilder.clause(
				segmentClass.call(parameterSegment1),
				segmentClass.call(parameterSegment2),
				segmentClass.call(parameterSegment3),
				segmentClass.call(parameterSegment4),
				segmentClass.call(parameterSegment5),
				segmentClass.call(parameterSegment6),
				connectsTo.call(parameterSegment1, parameterSegment2),
				connectsTo.call(parameterSegment2, parameterSegment3),
				connectsTo.call(parameterSegment3, parameterSegment4),
				connectsTo.call(parameterSegment4, parameterSegment5),
				connectsTo.call(parameterSegment5, parameterSegment6),
				monitoredBy.call(parameterSegment1, parameterSensor),
				monitoredBy.call(parameterSegment2, parameterSensor),
				monitoredBy.call(parameterSegment3, parameterSensor),
				monitoredBy.call(parameterSegment4, parameterSensor),
				monitoredBy.call(parameterSegment5, parameterSensor),
				monitoredBy.call(parameterSegment6, parameterSensor),
				parameterSegment1.notEquivalent(parameterSegment2),
				parameterSegment1.notEquivalent(parameterSegment3),
				parameterSegment1.notEquivalent(parameterSegment4),
				parameterSegment1.notEquivalent(parameterSegment5),
				parameterSegment1.notEquivalent(parameterSegment6),
				parameterSegment2.notEquivalent(parameterSegment3),
				parameterSegment2.notEquivalent(parameterSegment4),
				parameterSegment2.notEquivalent(parameterSegment5),
				parameterSegment2.notEquivalent(parameterSegment6),
				parameterSegment3.notEquivalent(parameterSegment4),
				parameterSegment3.notEquivalent(parameterSegment5),
				parameterSegment3.notEquivalent(parameterSegment6),
				parameterSegment4.notEquivalent(parameterSegment5),
				parameterSegment4.notEquivalent(parameterSegment6),
				parameterSegment5.notEquivalent(parameterSegment6)
		);
		connectedSegments = queryBuilder.build();



		QueryBuilder moveTrainToNextRoutePreconditionQueryBuilder = Query.builder("moveTrainToNextRoutePrecondition");
		moveTrainToNextRoutePreconditionQueryBuilder.clause(
				segmentClass.call(parameterSegment1),
				segmentClass.call(parameterSegment2),
				segmentClass.call(parameterSegment3)
		);


		moveTrainToNextRoutePreconditionPartialRelation = new PartialRelation("moveTrainToNextRoute_precondition", 8);

		RuleBuilder moveTrainToNextRouteRuleBuilder = Rule.builder("moveTrainToNextRoute");
		NodeVariable parameterTrain = moveTrainToNextRouteRuleBuilder.parameter("train");
		NodeVariable parameterFrom = moveTrainToNextRouteRuleBuilder.parameter("from");
		NodeVariable parameterTo = moveTrainToNextRouteRuleBuilder.parameter("to");
		NodeVariable parameterOldRoute = moveTrainToNextRouteRuleBuilder.parameter("oldRoute");
		NodeVariable parameterOldSemaphore = moveTrainToNextRouteRuleBuilder.parameter("oldSemaphore");
		NodeVariable parameterNewSemaphore = moveTrainToNextRouteRuleBuilder.parameter("newSemaphore");
		NodeVariable parameterGO = moveTrainToNextRouteRuleBuilder.parameter("GO");
		NodeVariable parameterSTOP = moveTrainToNextRouteRuleBuilder.parameter("STOP");

		moveTrainToNextRouteRuleBuilder
				.clause((newRoute, oldEntry) -> List.of(
						parameterGO.isConstant(this.SIGNAL_GO),
						parameterSTOP.isConstant(this.SIGNAL_STOP),
						trainClass.call(parameterTrain),
						trackElementClass.call(parameterFrom),
						trackElementClass.call(parameterTo),
						routeClass.call(parameterOldRoute),
						semaphoreClass.call(parameterOldSemaphore),
						semaphoreClass.call(parameterNewSemaphore),
						active.call(newRoute),
						connectsTo.call(parameterFrom, parameterTo),
						location.call(parameterTrain, parameterFrom),
						exit.call(parameterOldRoute, parameterNewSemaphore),
						semaphores.call(parameterTo, parameterNewSemaphore),
						routeEntry.call(newRoute, parameterTo),
						routeEntry.call(parameterOldRoute, parameterOldSemaphore),
						semaphores.call(oldEntry, parameterOldSemaphore)
				))
				.action(
						PartialActionLiterals.modify(location, FALSE, parameterTrain, parameterFrom),
						PartialActionLiterals.modify(location, TRUE, parameterTrain, parameterTo),
						PartialActionLiterals.modify(active, FALSE, parameterOldRoute),
						PartialActionLiterals.modify(signal, FALSE, parameterOldSemaphore, parameterSTOP),
						PartialActionLiterals.modify(signal, TRUE, parameterOldSemaphore, parameterGO),
						PartialActionLiterals.modify(signal, FALSE, parameterNewSemaphore, parameterGO),
						PartialActionLiterals.modify(signal, TRUE, parameterNewSemaphore, parameterSTOP)
				);

		var moveTrainToNextRouteRule = moveTrainToNextRouteRuleBuilder.build();

		moveTrainToNextRoute = new TransitionRule(moveTrainToNextRoutePreconditionPartialRelation,
				moveTrainToNextRouteRule);

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
		var reasoningBuilder = storeBuilder.getAdapter(ReasoningBuilder.class);

		posLengthLifted = reasoningBuilder.lift(Modality.MUST, Concreteness.PARTIAL, posLength);
		switchMonitoredLifted = reasoningBuilder.lift(Modality.MUST, Concreteness.PARTIAL, switchMonitored);
		routeSensorLifted = reasoningBuilder.lift(Modality.MUST, Concreteness.PARTIAL, routeSensor);
		switchSetLifted = reasoningBuilder.lift(Modality.MUST, Concreteness.PARTIAL, switchSet);
		semaphoreNeighborLifted = reasoningBuilder.lift(Modality.MUST, Concreteness.PARTIAL, semaphoreNeighbor);
		connectedSegmentsLifted = reasoningBuilder.lift(Modality.MUST, Concreteness.PARTIAL, connectedSegments);

		queries.add(posLengthLifted);
		queries.add(switchMonitoredLifted);
		queries.add(routeSensorLifted);
		queries.add(switchSetLifted);
		queries.add(semaphoreNeighborLifted);
		queries.add(connectedSegmentsLifted);


		switch (scenario) {
		case SIMULATE -> {
			storeBuilder = storeBuilder
				.with(TransitionSystemAdapter.builder()
						.transition(activateRoute)
						.transition(placeTrainOnRoute)
						.transition(changeSwitch)
						.transition(moveTrainOnRoute)
						.transition(activateNextRoute)
						.transition(moveTrainToNextRoute)
						.accept(_ -> () -> false) // full state space exploration
				)
				.with(new PredicateTranslator(
						activateRoutePreconditionPartialRelation,
						activateRoute.rule().getPrecondition(),
						List.of(routeClass), Set.of(), true, UNKNOWN))
				.with(new PredicateTranslator(
						placeTrainOnRoutePreconditionPartialRelation,
						placeTrainOnRoute.rule().getPrecondition(),
						List.of(segmentClass, semaphoreClass, signalEnum), Set.of(), true, UNKNOWN))
				.with(new PredicateTranslator(
						changeSwitchPreconditionPartialRelation,
						changeSwitch.rule().getPrecondition(),
						List.of(switchClass, positionEnum, positionEnum), Set.of(), true, UNKNOWN))
				.with(new PredicateTranslator(
						moveTrainOnRoutePreconditionPartialRelation,
						moveTrainOnRoute.rule().getPrecondition(),
						List.of(trainClass, trackElementClass, trackElementClass), Set.of(), true, UNKNOWN))
				.with(new PredicateTranslator(
						activateNextRoutePreconditionPartialRelation,
						activateNextRoute.rule().getPrecondition(),
						List.of(routeClass), Set.of(), true, UNKNOWN))
				.with(new PredicateTranslator(
						moveTrainToNextRoutePreconditionPartialRelation,
						moveTrainToNextRoute.rule().getPrecondition(),
						List.of(trainClass, trackElementClass, trackElementClass, routeClass, semaphoreClass,
								semaphoreClass, signalEnum, signalEnum),
						Set.of(), true, UNKNOWN))
					.with(new PredicateTranslator(
							repairSwitchSetPreconditionPartialRelation,
							repairSwitchSet.rule().getPrecondition(),
							List.of(switchClass, positionEnum, positionEnum), Set.of(), true, UNKNOWN));
			fireOneTransitionRules.add(activateRoute.rule().getName());
			fireOneTransitionRules.add(placeTrainOnRoute.rule().getName());
			fireOneTransitionRules.add(changeSwitch.rule().getName());
			fireOneTransitionRules.add(moveTrainOnRoute.rule().getName());
			fireOneTransitionRules.add(activateNextRoute.rule().getName());
			fireOneTransitionRules.add(moveTrainToNextRoute.rule().getName());
			fireOneTransitionRules.add(repairSwitchSet.rule().getName());
		}
		case INJECT -> {
			storeBuilder = storeBuilder
				.with(TransitionSystemAdapter.builder()
						.transition(injectConnectedSegments)
						.transition(injectPosLength)
						.transition(injectRouteSensor)
						.transition(injectSemaphoreNeighbor)
						.transition(injectSwitchMonitored)
						.transition(injectSwitchSet)
						.accept(_ -> () -> false) // full state space exploration
				)
				.with(new PredicateTranslator(
						injectConnectedSegmentsPreconditionPartialRelation,
						injectConnectedSegments.rule().getPrecondition(),
						List.of(sensorClass, segmentClass, segmentClass), Set.of(), true, UNKNOWN))
				.with(new PredicateTranslator(
						injectPosLengthPreconditionPartialRelation,
						injectPosLength.rule().getPrecondition(),
						List.of(segmentClass), Set.of(), true, UNKNOWN))
				.with(new PredicateTranslator(
						injectRouteSensorPreconditionPartialRelation,
						injectRouteSensor.rule().getPrecondition(),
						List.of(routeClass, sensorClass), Set.of(), true, UNKNOWN))
				.with(new PredicateTranslator(
						injectSemaphoreNeighborPreconditionPartialRelation,
						injectSemaphoreNeighbor.rule().getPrecondition(),
						List.of(semaphoreClass, routeClass), Set.of(), true, UNKNOWN))
				.with(new PredicateTranslator(
						injectSwitchMonitoredPreconditionPartialRelation,
						injectSwitchMonitored.rule().getPrecondition(),
						List.of(switchClass, sensorClass), Set.of(), true, UNKNOWN))
				.with(new PredicateTranslator(
						injectSwitchSetPreconditionPartialRelation,
						injectSwitchSet.rule().getPrecondition(),
						List.of(switchClass, positionEnum, positionEnum), Set.of(), true, UNKNOWN));
			fireOneTransitionRules.add(injectConnectedSegments.rule().getName());
			fireOneTransitionRules.add(injectPosLength.rule().getName());
			fireOneTransitionRules.add(injectRouteSensor.rule().getName());
			fireOneTransitionRules.add(injectSemaphoreNeighbor.rule().getName());
			fireOneTransitionRules.add(injectSwitchSet.rule().getName());
			fireAllTransitionRules.add(injectSwitchMonitored.rule().getName());
		}
		case REPAIR -> {
			storeBuilder = storeBuilder
					.with(TransitionSystemAdapter.builder()
							.transition(repairConnectedSegments)
							.transition(repairPosLength)
							.transition(repairRouteSensor)
							.transition(repairSemaphoreNeighbor)
							.transition(repairSwitchMonitored)
							.transition(repairSwitchSet)
							.accept(_ -> () -> false) // full state space exploration
					)
					.with(new PredicateTranslator(
							repairConnectedSegmentsPreconditionPartialRelation,
							repairConnectedSegments.rule().getPrecondition(),
							List.of(segmentClass, segmentClass, segmentClass), Set.of(), true, UNKNOWN))
					.with(new PredicateTranslator(
							repairPosLengthPreconditionPartialRelation,
							repairPosLength.rule().getPrecondition(),
							List.of(segmentClass), Set.of(), true, UNKNOWN))
					.with(new PredicateTranslator(
							repairRouteSensorPreconditionPartialRelation,
							repairRouteSensor.rule().getPrecondition(),
							List.of(routeClass, sensorClass), Set.of(), true, UNKNOWN))
					.with(new PredicateTranslator(
							repairSemaphoreNeighborPreconditionPartialRelation,
							repairSemaphoreNeighbor.rule().getPrecondition(),
							List.of(semaphoreClass, routeClass), Set.of(), true, UNKNOWN))
					.with(new PredicateTranslator(
							repairSwitchMonitoredPreconditionPartialRelation,
							repairSwitchMonitored.rule().getPrecondition(),
							List.of(switchClass), Set.of(), true, UNKNOWN))
					.with(new PredicateTranslator(
							repairSwitchSetPreconditionPartialRelation,
							repairSwitchSet.rule().getPrecondition(),
							List.of(switchClass, positionEnum, positionEnum), Set.of(), true, UNKNOWN));

			fireOneTransitionRules.add(repairConnectedSegments.rule().getName());
			fireOneTransitionRules.add(repairPosLength.rule().getName());
			fireOneTransitionRules.add(repairRouteSensor.rule().getName());
			fireOneTransitionRules.add(repairSemaphoreNeighbor.rule().getName());
			fireOneTransitionRules.add(repairSwitchMonitored.rule().getName());
			fireOneTransitionRules.add(repairSwitchSet.rule().getName());
		}
		default -> throw new IllegalArgumentException("Invalid scenario for this measurement: " + scenario);
		}

		String pathToCsvFolder = "input/models/trainbenchmark2";

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
			String relationName = csvFile.getName().replace(fileNamePrefix, "").replace(".csv", "");
			System.out.println("Loading class: " + relationName);
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
			String relationName = csvFile.getName().replace(fileNamePrefix, "").replace(".csv", "");
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
			String relationName = csvFile.getName().replace(fileNamePrefix, "").replace(".csv", "");
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
					inputMap.add(Tuple.of(Integer.parseInt(values[0]) + firstFreeId, enumValue.get(0)));
				}
			} catch (java.io.IOException e) {
				e.printStackTrace();
			}
		}

////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

		// TODO: This is a hack to get the number of nodes in the model. We should find a better way to do this.
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

		switch (scenario) {
		case SIMULATE -> {
			modelSeedBuilder.seed(injectConnectedSegmentsPreconditionPartialRelation, builder -> builder.reducedValue(UNKNOWN));
			modelSeedBuilder.seed(injectPosLengthPreconditionPartialRelation, builder -> builder.reducedValue(UNKNOWN));
			modelSeedBuilder.seed(injectRouteSensorPreconditionPartialRelation, builder -> builder.reducedValue(UNKNOWN));
			modelSeedBuilder.seed(injectSemaphoreNeighborPreconditionPartialRelation, builder -> builder.reducedValue(UNKNOWN));
			modelSeedBuilder.seed(injectSwitchMonitoredPreconditionPartialRelation, builder -> builder.reducedValue(UNKNOWN));
			modelSeedBuilder.seed(injectSwitchSetPreconditionPartialRelation, builder -> builder.reducedValue(UNKNOWN));

		}
		case INJECT -> {
			modelSeedBuilder.seed(injectConnectedSegmentsPreconditionPartialRelation, builder -> builder.reducedValue(UNKNOWN));
			modelSeedBuilder.seed(injectPosLengthPreconditionPartialRelation, builder -> builder.reducedValue(UNKNOWN));
			modelSeedBuilder.seed(injectRouteSensorPreconditionPartialRelation, builder -> builder.reducedValue(UNKNOWN));
			modelSeedBuilder.seed(injectSemaphoreNeighborPreconditionPartialRelation, builder -> builder.reducedValue(UNKNOWN));
			modelSeedBuilder.seed(injectSwitchMonitoredPreconditionPartialRelation, builder -> builder.reducedValue(UNKNOWN));
			modelSeedBuilder.seed(injectSwitchSetPreconditionPartialRelation, builder -> builder.reducedValue(UNKNOWN));

		}
		case REPAIR -> {
			modelSeedBuilder.seed(repairConnectedSegmentsPreconditionPartialRelation, builder -> builder.reducedValue(UNKNOWN));
			modelSeedBuilder.seed(repairPosLengthPreconditionPartialRelation, builder -> builder.reducedValue(UNKNOWN));
			modelSeedBuilder.seed(repairRouteSensorPreconditionPartialRelation, builder -> builder.reducedValue(UNKNOWN));
			modelSeedBuilder.seed(repairSemaphoreNeighborPreconditionPartialRelation, builder -> builder.reducedValue(UNKNOWN));
			modelSeedBuilder.seed(repairSwitchMonitoredPreconditionPartialRelation, builder -> builder.reducedValue(UNKNOWN));
			modelSeedBuilder.seed(repairSwitchSetPreconditionPartialRelation, builder -> builder.reducedValue(UNKNOWN));
		}

		}

		this.modelSeed = this.modelSeedBuilder.build();
		super.init();
	}

	@Override
	protected boolean renderTransitionsToAlreadyVisitedStates() {
		return true;
	}

	public MeasurementResult runQueries(int steps) {
		var queryAdapter = model.getAdapter(ModelQueryAdapter.class);
		var resultSetPosLength = queryAdapter.getResultSet(posLengthLifted);
		var resultSetSwitchMonitored = queryAdapter.getResultSet(switchMonitoredLifted);
		var resultSetRouteSensor = queryAdapter.getResultSet(routeSensorLifted);
		var resultSetSwitchSet = queryAdapter.getResultSet(switchSetLifted);
		var resultSetSemaphoreNeighbor = queryAdapter.getResultSet(semaphoreNeighborLifted);
		var resultSetConnectedSegments = queryAdapter.getResultSet(connectedSegmentsLifted);
		System.out.println("Result set sizes:");
		System.out.println("posLength: " + resultSetPosLength.size());
		System.out.println("switchMonitored: " + resultSetSwitchMonitored.size());
		System.out.println("routeSensor: " + resultSetRouteSensor.size());
		System.out.println("switchSet: " + resultSetSwitchSet.size());
		System.out.println("semaphoreNeighbor: " + resultSetSemaphoreNeighbor.size());
		System.out.println("connectedSegments: " + resultSetConnectedSegments.size());
		var transformationIndex = 0;
		for (var transition : measuredTransitionRules) {
			for (int i = 0; i < steps; i++) {
				List<Activation> activations = new ArrayList<>();
				var activationCursor = transition.transition().getAllActivationsAsResultSet().getAll();
				var tupleIndex = 0;
				while (activationCursor.move()) {
					var activation = new Activation(transition, activationCursor.getKey(), transformationIndex, tupleIndex);
					activations.add(activation);
					tupleIndex++;
				}
				if (activations.isEmpty()) {
					System.out.println("Transition " + transition + " has no " +
							"activations at step " + i);
					break;
				}
				var randomActivation = activations.get((random.nextInt(activations.size())));
				fireTransition(randomActivation);
			}
			transformationIndex++;
		}
		System.out.println("Result set sizes:");
		System.out.println("posLength: " + resultSetPosLength.size());
		System.out.println("switchMonitored: " + resultSetSwitchMonitored.size());
		System.out.println("routeSensor: " + resultSetRouteSensor.size());
		System.out.println("switchSet: " + resultSetSwitchSet.size());
		System.out.println("semaphoreNeighbor: " + resultSetSemaphoreNeighbor.size());
		System.out.println("connectedSegments: " + resultSetConnectedSegments.size());
		return MeasurementResult.MAX_STEPS_REACHED;
	}
}

