package tools.refinery.measurement;

import tools.refinery.logic.term.Variable;
import tools.refinery.logic.term.cardinalityinterval.CardinalityIntervals;
import tools.refinery.logic.term.truthvalue.TruthValue;
import tools.refinery.store.dse.modification.ModificationAdapter;
import tools.refinery.store.dse.propagation.PropagationAdapter;
import tools.refinery.store.dse.transition.DesignSpaceExplorationAdapter;
import tools.refinery.store.dse.transition.Rule;
import tools.refinery.store.dse.transition.statespace.ActivationStore;
import tools.refinery.store.dse.transition.statespace.StateSpaceStore;
import tools.refinery.store.map.Version;
import tools.refinery.store.model.Model;
import tools.refinery.store.model.ModelStore;
import tools.refinery.store.model.ModelStoreBuilder;
import tools.refinery.store.query.ModelQueryAdapter;
import tools.refinery.store.query.interpreter.QueryInterpreterAdapter;
import tools.refinery.store.reasoning.ReasoningAdapter;
import tools.refinery.store.reasoning.ReasoningStoreAdapter;
import tools.refinery.store.reasoning.actions.PartialActionLiterals;
import tools.refinery.store.reasoning.representation.PartialRelation;
import tools.refinery.store.reasoning.seed.ModelSeed;
import tools.refinery.store.reasoning.translator.containment.ContainmentHierarchyTranslator;
import tools.refinery.store.reasoning.translator.metamodel.Metamodel;
import tools.refinery.store.reasoning.translator.metamodel.MetamodelBuilder;
import tools.refinery.store.reasoning.translator.metamodel.MetamodelTranslator;
import tools.refinery.store.reasoning.translator.multiobject.MultiObjectTranslator;
import tools.refinery.store.reasoning.translator.predicate.PredicateTranslator;
import tools.refinery.store.statecoding.StateCoderAdapter;
import tools.refinery.store.transition.system.TransitionSystemAdapter;
import tools.refinery.store.transition.system.statespace.TransitionRule;
import tools.refinery.store.transition.system.strategy.TransitionSystemStoreManager;
import tools.refinery.store.tuple.Tuple;
import tools.refinery.visualization.ModelVisualizerAdapter;
import tools.refinery.visualization.internal.FileFormat;

import java.util.*;

import static tools.refinery.logic.literal.Literals.not;
import static tools.refinery.logic.term.truthvalue.TruthValue.*;

public class VehicleMovingMeasurement {
	private static final boolean visualize = false;

    PartialRelation vehicle = new PartialRelation("Vehicle", 1);
    PartialRelation lane = new PartialRelation("Lane", 1);
    PartialRelation on = new PartialRelation("on", 2);
	PartialRelation onInvalidMultiplicity = new PartialRelation("on::invalidMultiplicity", 1);
    PartialRelation followedBy = new PartialRelation("followedBy", 2);
	PartialRelation followedByInvalidMultiplicity = new PartialRelation("followedBy::invalidMultiplicity", 1);
    PartialRelation left = new PartialRelation("left", 2);
	PartialRelation leftInvalidMultiplicity = new PartialRelation("left::invalidMultiplicity", 1);
    PartialRelation right = new PartialRelation("right", 2);
	PartialRelation rightInvalidMultiplicity = new PartialRelation("right::invalidMultiplicity", 1);


	MetamodelBuilder builder = Metamodel.builder()
			.type(vehicle)
			.type(lane)
			.reference(on, referenceBuilder -> referenceBuilder
					.source(vehicle)
					.target(lane)
					.multiplicity(CardinalityIntervals.ONE, onInvalidMultiplicity)
					.defaultValue(TruthValue.FALSE))
			.reference(followedBy, referenceBuilder -> referenceBuilder
					.source(lane)
					.target(lane)
					.multiplicity(CardinalityIntervals.LONE, followedByInvalidMultiplicity)
					.defaultValue(TruthValue.FALSE))
			.reference(left, referenceBuilder -> referenceBuilder
					.source(lane)
					.target(lane)
					.opposite(right)
					.multiplicity(CardinalityIntervals.LONE, leftInvalidMultiplicity)
					.defaultValue(TruthValue.FALSE))
			.reference(right, referenceBuilder -> referenceBuilder
					.source(lane)
					.target(lane)
					.opposite(left)
					.multiplicity(CardinalityIntervals.LONE, rightInvalidMultiplicity)
					.defaultValue(TruthValue.FALSE));
	Metamodel metamodel = builder.build();

    PartialRelation removeVehiclePreconditionPartialRelation = new PartialRelation("removeVehicle_precondition", 2);
    TransitionRule removeVehicle = new TransitionRule(removeVehiclePreconditionPartialRelation,
            Rule.of("removeVehicle", (builder, v, l) -> builder
            .clause(
                    vehicle.call(v),
                    on.call(v, l),
                    lane.call(l),
                    not(followedBy.call(l, Variable.of()))
            )
            .action(
					PartialActionLiterals.modify(on, FALSE, v, l)
            )));

    PartialRelation moveVehicleForwardPreconditionPartialRelation = new PartialRelation("moveVehicleForward_precondition", 3);
    TransitionRule moveVehicleForward = new TransitionRule(moveVehicleForwardPreconditionPartialRelation,
            Rule.of("moveVehicleForward", (builder, v, from, to) -> builder
            .clause(
                    vehicle.call(v),
                    on.call(v, from),
                    lane.call(from),
                    lane.call(to),
                    followedBy.call(from, to)
            )
            .action(
					PartialActionLiterals.modify(on, FALSE, v, from),
					PartialActionLiterals.modify(on, TRUE, v, to)
            )));

    PartialRelation moveVehicleLeftPreconditionPartialRelation = new PartialRelation("moveVehicleLeft_precondition", 3);
    TransitionRule moveVehicleLeft = new TransitionRule(moveVehicleLeftPreconditionPartialRelation,
            Rule.of("moveVehicleLeft", (builder, v, from, to) -> builder
            .clause(
                    vehicle.call(v),
                    on.call(v, from),
                    lane.call(from),
                    lane.call(to),
                    left.call(from, to)
            )
            .action(
					PartialActionLiterals.modify(on, FALSE, v, from),
					PartialActionLiterals.modify(on, TRUE, v, to)
            )));

    PartialRelation moveVehicleRightPreconditionPartialRelation = new PartialRelation("moveVehicleRight_precondition", 3);
    TransitionRule moveVehicleRight = new TransitionRule(moveVehicleRightPreconditionPartialRelation,
            Rule.of("moveVehicleRight", (builder, v, from, to) -> builder
            .clause(
                    vehicle.call(v),
                    on.call(v, from),
                    lane.call(from),
                    lane.call(to),
                    right.call(from, to)
            )
            .action(
					PartialActionLiterals.modify(on, FALSE, v, from),
					PartialActionLiterals.modify(on, TRUE, v, to)
            )));

    PartialRelation placeVehiclePreconditionPartialRelation = new PartialRelation("placeVehicle_precondition", 2);
    TransitionRule placeVehicle = new TransitionRule(placeVehiclePreconditionPartialRelation,
            Rule.of("placeVehicle", (builder, v, l) -> builder
            .clause(
                    vehicle.call(v),
                    lane.call(l),
                    not(on.call(v, Variable.of()))
            )
            .action(
					PartialActionLiterals.modify(on, TRUE, v, l)
            )));

    ModelStoreBuilder storeBuilder = ModelStore.builder()
            .with(QueryInterpreterAdapter.builder())
            .with(PropagationAdapter.builder())
            .with(StateCoderAdapter.builder())
            .with(ModificationAdapter.builder())
            .with(DesignSpaceExplorationAdapter.builder())
            .with(ReasoningAdapter.builder())
            .with(TransitionSystemAdapter.builder()
                    .transition(moveVehicleForward)
                    .transition(moveVehicleLeft)
                    .transition(moveVehicleRight)
                    .transition(removeVehicle)
                    .transition(placeVehicle)
                    .accept(_ -> () -> false) // full state space exploration
            )
			.with(new MultiObjectTranslator())
			.with(new MetamodelTranslator(metamodel))
//            .with(PartialRelationTranslator.of(on).symbol(onStorage))
//            .with(PartialRelationTranslator.of(vehicle).symbol(vehicleStorage))
//            .with(PartialRelationTranslator.of(lane).symbol(laneStorage))
//            .with(PartialRelationTranslator.of(followedBy).symbol(followedByStorage))
//            .with(PartialRelationTranslator.of(left).symbol(leftStorage))
//            .with(PartialRelationTranslator.of(right).symbol(rightStorage))
			.with(new PredicateTranslator(moveVehicleForwardPreconditionPartialRelation, moveVehicleForward.rule().getPrecondition(),
					List.of(vehicle, lane, lane), Set.of(), true, UNKNOWN))
			.with(new PredicateTranslator(moveVehicleLeftPreconditionPartialRelation, moveVehicleLeft.rule().getPrecondition(),
					List.of(vehicle, lane, lane), Set.of(), true, UNKNOWN))
			.with(new PredicateTranslator(moveVehicleRightPreconditionPartialRelation, moveVehicleRight.rule().getPrecondition(),
					List.of(vehicle, lane, lane), Set.of(), true, UNKNOWN))
			.with(new PredicateTranslator(removeVehiclePreconditionPartialRelation, removeVehicle.rule().getPrecondition(),
					List.of(vehicle, lane), Set.of(), true, UNKNOWN))
			.with(new PredicateTranslator(placeVehiclePreconditionPartialRelation, placeVehicle.rule().getPrecondition(),
					List.of(vehicle, lane), Set.of(), true, UNKNOWN));

	ModelStore store;
    ModelSeed.Builder modelSeedBuilder;
    private ModelQueryAdapter queryEngine;
    private PropagationAdapter propagationAdapter;
    private DesignSpaceExplorationAdapter dseAdapter;
    private TransitionSystemAdapter tsAdapter;
    private Model model;
//    private Version state;
    private final Random random = new Random();
    private Version currentVersion;
    private ModelVisualizerAdapter modelVisualizerAdapter;
    private StateSpaceStore stateSpaceStore;
    private StateCoderAdapter stateCoderAdapter;


    public void init(int size) {
		if (visualize) {
			storeBuilder = storeBuilder.with(ModelVisualizerAdapter.builder()
					.withOutputPath("test_output")
					.withFormat(FileFormat.SVG)
					.saveStates()
					.saveDesignSpace()
			);
		}
		store = storeBuilder.build();
        var nodeCount = size * (size + 1);
        this.modelSeedBuilder = ModelSeed.builder(nodeCount)
                .seed(MultiObjectTranslator.COUNT_SYMBOL, builder -> builder
                        .reducedValue(CardinalityIntervals.ONE)
                )
				.seed(ContainmentHierarchyTranslator.CONTAINED_SYMBOL, builder ->
						builder.reducedValue(UNKNOWN))
				.seed(ContainmentHierarchyTranslator.CONTAINER_SYMBOL, builder ->
						builder.reducedValue(UNKNOWN))
				.seed(ContainmentHierarchyTranslator.CONTAINS_SYMBOL, builder ->
						builder.reducedValue(UNKNOWN))
                .seed(lane, builder ->
                        {
//							System.out.println("Seeding lane");
                            for (int i = 0; i < size * size; i++) {
                                builder.put(Tuple.of(i), TruthValue.TRUE);
                            }
							builder.reducedValue(UNKNOWN);
                        }
                )
                .seed(vehicle, builder ->
                        {
//							System.out.println("Seeding vehicle");
                            for (int i = size * size; i < nodeCount; i++) {
                                builder.put(Tuple.of(i), TruthValue.TRUE);
                            }
							builder.reducedValue(UNKNOWN);
                        }
                )
                .seed(followedBy, builder ->
                        {
//							System.out.println("Seeding followedBy");
                            for (int i = 0; i < size; i++) {
                                for (int j = 0; j < size; j++) {
                                    builder.put(Tuple.of(i * size + j, i * size + (j + 1) % size), TruthValue.TRUE);
                                }
                            }
							builder.reducedValue(TruthValue.FALSE);
                        }
                )
                .seed(right, builder ->
                        {
//							System.out.println("Seeding right");
                            for (int i = 0; i < size ; i++) {
                                for (int j = 0; j < size; j++) {
                                    builder.put(Tuple.of(i * size + j, ((i + 1) % size) * size + j), TruthValue.TRUE);
                                }
                            }
							builder.reducedValue(TruthValue.FALSE);
                        }
                )
                .seed(left, builder ->
                        {
//							System.out.println("Seeding left");
                            for (int i = 0; i < size; i++) {
                                for (int j = 0; j < size ; j++) {
									builder.put(Tuple.of(((i + 1) % size) * size + j, i * size + j), TruthValue.TRUE);
                                }
                            }
							builder.reducedValue(TruthValue.FALSE);
                        }
                )
                .seed(on, builder ->
                        {
//							System.out.println("Seeding on");
                            for (int i = 0; i < size; i++) {
                                builder.put(Tuple.of(size * size + i, i * size), TruthValue.TRUE);
//								builder.put(Tuple.of(size * size + i, 0), TruthValue.TRUE);
                            }
							builder.reducedValue(TruthValue.FALSE);
                        }
                )
                .seed(moveVehicleForwardPreconditionPartialRelation, builder -> builder.reducedValue(TruthValue.UNKNOWN))
                .seed(moveVehicleLeftPreconditionPartialRelation, builder -> builder.reducedValue(TruthValue.UNKNOWN))
                .seed(moveVehicleRightPreconditionPartialRelation, builder -> builder.reducedValue(TruthValue.UNKNOWN))
                .seed(removeVehiclePreconditionPartialRelation, builder -> builder.reducedValue(TruthValue.UNKNOWN))
                .seed(placeVehiclePreconditionPartialRelation, builder -> builder.reducedValue(TruthValue.UNKNOWN))
        ;

//        System.out.println("building model seed with " + nodeCount + " nodes");
        var seed = this.modelSeedBuilder.build();
//        System.out.println("model seed built, creating model");
        this.model = store.getAdapter(ReasoningStoreAdapter.class).tryCreateInitialModel(seed).model();
//        System.out.println("model created, committing initial state");
        this.queryEngine = model.getAdapter(ModelQueryAdapter.class);
        this.currentVersion = model.commit();
//        System.out.println("initial state committed, flushing changes");
        this.queryEngine.flushChanges();
//        System.out.println("changes flushed, getting adapters");
        this.propagationAdapter = model.getAdapter(PropagationAdapter.class);
        this.dseAdapter = model.getAdapter(DesignSpaceExplorationAdapter.class);
        this.tsAdapter = model.getAdapter(TransitionSystemAdapter.class);


		if (visualize) {
			this.modelVisualizerAdapter = model.getAdapter(ModelVisualizerAdapter.class);
			this.stateSpaceStore = model.getAdapter(DesignSpaceExplorationAdapter.class).getStateSpaceStore();
			this.stateCoderAdapter = model.getAdapter(StateCoderAdapter.class);

//			stateSpaceStore.addState(currentVersion, stateCoderAdapter.calculateModelCode(), false, false);
		}
    }

    public void run(int steps) {
//        System.out.println("Running " + steps + " steps of random exploration");
        List<Activation> activations = getActivations();
        for (int i = 0; i < steps && !activations.isEmpty(); i++) {
//            System.out.println("Step " + i + ": " + activations.size() + " activations available.");
            var randomActivation = activations.get((random.nextInt(activations.size())));
            step(randomActivation);
            activations = getActivations();
        }
    }

    public void finish() {
		if (visualize) {
			modelVisualizerAdapter.visualize(stateSpaceStore, false);
		}
        model.close();
    }

    public void runTransitionSystem() {
		var manager = new TransitionSystemStoreManager(store);
		manager.startExploration(this.currentVersion);
//        modelVisualizerAdapter.visualize(store.getAdapter(DesignSpaceExplorationStoreAdapter.class).getStateSpaceStore(), true);
    }

    private void step(Activation activation) {
		Version lastVersion;
		if (visualize) {
			lastVersion = this.currentVersion;
		}
        activation.fire();
        propagationAdapter.propagate();
        dseAdapter.checkAccept();
        this.currentVersion = model.commit();
		if (visualize) {
			stateSpaceStore.addState(currentVersion, stateCoderAdapter.calculateModelCode(), false, false);
			stateSpaceStore.addTransition(lastVersion, this.currentVersion, new ActivationStore.VisitResult(
					true, true, activation.transformationIndex(), activation.tupleIndex()));
		}
        queryEngine.flushChanges();
    }

    private List<Activation> getActivations() {
        List<Activation> activationList = new ArrayList<>();
        var transformationIndex = 0;
        for (var transformation : tsAdapter.getTransitions()) {
            var activationCursor = transformation.getAllActivationsAsResultSet().getAll();
            var tupleIndex = 0;
            while (activationCursor.move()) {
                activationList.add(new Activation(transformation, activationCursor.getKey(), transformationIndex, tupleIndex));
                tupleIndex++;
            }
            transformationIndex++;
        }
//		System.out.println(activationList.size() + " activations found: " + activationList);
        return activationList;
    }
}

