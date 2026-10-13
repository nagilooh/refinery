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

import java.math.BigInteger;
import java.util.List;
import java.util.Set;

import static tools.refinery.logic.term.truthvalue.TruthValue.*;

public class TrainBenchmarkMeasurement extends MeasurementBase {

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

			.type(position)
			.type(signal)

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
					.target(signal)
					.multiplicity(CardinalityIntervals.LONE, signalInvalidMultiplicity)
					.defaultValue(FALSE))
			.reference(follows, referenceBuilder -> referenceBuilder
					.source(routeClass)
					.target(switchPositionClass)
					.opposite(routeClass)
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
					.target(position)
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
					.target(position)
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

//
//    PartialRelation stateTransitionPreconditionPartialRelation = new PartialRelation("stateTransition_precondition", 3);
//    TransitionRule stateTransition = new TransitionRule(stateTransitionPreconditionPartialRelation,
//            Rule.of("stateTransition", (builder, t, from, to) -> builder
//            .clause(
//                    transition.call(t),
//					state.call(from),
//					state.call(to),
//                    source.call(t, from),
//                    target.call(t, to),
//					active.call(from)
//            )
//            .action(
//					PartialActionLiterals.modify(active, FALSE, from),
//					PartialActionLiterals.modify(active, TRUE, to)
//            )));

    ModelSeed.Builder modelSeedBuilder;

	public void init(int size, boolean visualize) {
		this.visualize = visualize;
		if (visualize) {
			visualizationOutputPath = "output_visualization/trainbenchmark_" + size;
		}

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
//						.transition(stateTransition)
						.accept(_ -> () -> false) // full state space exploration
				)
				.with(new MultiObjectTranslator())
				.with(new MetamodelTranslator(metamodel))
//				.with(new PredicateTranslator(stateTransitionPreconditionPartialRelation, stateTransition.rule().getPrecondition(),
//						List.of(transition, state, state), Set.of(), true, UNKNOWN))

				.with(new BasePredicateTranslator(active, List.of(routeClass), Set.of(), UNKNOWN,
						new ConcretizationSettings(true, true)));



        var nodeCount = 10; // To be adjusted based on the size parameter or other logic
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

//		Add seed for each class and reference with values from csv
		// Example:
		//
		//	.seed(source, builder ->
		//	        {
		//				builder.put(Tuple.of(t1, redState), TruthValue.TRUE);
		//				builder.put(Tuple.of(t2, redYellowState), TruthValue.TRUE);
		//				builder.put(Tuple.of(t3, greenState), TruthValue.TRUE);
		//				builder.put(Tuple.of(t4, yellowState), TruthValue.TRUE);
		//				builder.reducedValue(TruthValue.FALSE);
		//	        }
		//	)

//		Iterate over csv files in folder and add seeds for each class and reference based on the data in the csv files
		// For example, if you have a csv file for routes, you can read the file and add seeds for the routes reference
		// Similarly, you can read other csv files and add seeds for other classes and references
		// You can use a library like OpenCSV or Apache Commons CSV to read the csv files

		String pathToCsvFolder = "/input/models/trainbenchmark/size-1"; // Replace with the actual path to your
		// CSV folder
		for (var csvFile :
				new java.io.File(pathToCsvFolder).listFiles((dir, name) -> name.startsWith("railway-batch-1") && name.endsWith(".csv"))) {
			String relationName = csvFile.getName().replace(".csv", "");
//			find the corresponding PartialRelation for the relationName
			PartialRelation parialRelation = null;
			switch (relationName) {
//				case "RailwayContainer" -> parialRelation = railwayContainerClass;
//				case "Region" -> parialRelation = regionClass;
//				case "Semaphore" -> parialRelation = semaphoreClass;
//				case "Route" -> parialRelation = routeClass;
//				case "SwitchPosition" -> parialRelation = switchPositionClass;
//				case "TrackElement" -> parialRelation = trackElementClass;


				case "connectsTo" -> parialRelation = connectsTo;
				case "entry" -> parialRelation = entry;
				case "exit" -> parialRelation = exit;
				case "follows" -> parialRelation = follows;
				case "monitoredBy" -> parialRelation = monitoredBy;
				case "Region" -> parialRelation = regionClass;
				case "requires" -> parialRelation = requires;
				case "Route" -> parialRelation = routeClass;
				case "Segment" -> parialRelation = segmentClass;
				case "Semaphore" -> parialRelation = semaphoreClass;
				case "Sensor" -> parialRelation = sensorClass;
				case "Switch" -> parialRelation = switchClass;
				case "SwitchPosition" -> parialRelation = switchPositionClass;
				case "target" -> parialRelation = target;
			}
			modelSeedBuilder.seed(parialRelation, builder -> {
				try (java.io.BufferedReader br = new java.io.BufferedReader(new java.io.FileReader(csvFile))) {
					String line;
					while ((line = br.readLine()) != null) {
						String[] values = line.split(",");
						builder.put(Tuple.of(Integer.parseInt(values[0])), TruthValue.TRUE);
					}
				} catch (java.io.IOException e) {
					e.printStackTrace();
				}
				builder.reducedValue(TruthValue.FALSE);
			});
		}

//        System.out.println("building model seed with " + nodeCount + " nodes");
        this.modelSeed = this.modelSeedBuilder.build();
		super.init();
    }

	@Override
	protected boolean renderTransitionsToAlreadyVisitedStates() {
		return true;
	}
}

