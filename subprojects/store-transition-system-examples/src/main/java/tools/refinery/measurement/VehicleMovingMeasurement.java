package tools.refinery.measurement;

import tools.refinery.logic.term.Variable;
import tools.refinery.logic.term.cardinalityinterval.CardinalityIntervals;
import tools.refinery.store.dse.modification.ModificationAdapter;
import tools.refinery.store.dse.propagation.PropagationAdapter;
import tools.refinery.store.dse.transition.DesignSpaceExplorationAdapter;
import tools.refinery.store.dse.transition.Rule;
import tools.refinery.store.model.ModelStore;
import tools.refinery.store.query.interpreter.QueryInterpreterAdapter;
import tools.refinery.store.reasoning.ReasoningAdapter;
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
import tools.refinery.store.tuple.Tuple;

import java.util.*;

import static tools.refinery.logic.literal.Literals.not;
import static tools.refinery.logic.term.truthvalue.TruthValue.*;

public class VehicleMovingMeasurement extends MeasurementBase {

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
					.defaultValue(FALSE))
			.reference(followedBy, referenceBuilder -> referenceBuilder
					.source(lane)
					.target(lane)
					.multiplicity(CardinalityIntervals.LONE, followedByInvalidMultiplicity)
					.defaultValue(FALSE))
			.reference(left, referenceBuilder -> referenceBuilder
					.source(lane)
					.target(lane)
					.opposite(right)
					.multiplicity(CardinalityIntervals.LONE, leftInvalidMultiplicity)
					.defaultValue(FALSE))
			.reference(right, referenceBuilder -> referenceBuilder
					.source(lane)
					.target(lane)
					.opposite(left)
					.multiplicity(CardinalityIntervals.LONE, rightInvalidMultiplicity)
					.defaultValue(FALSE));
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

ModelSeed.Builder modelSeedBuilder;

    public void init(int size, MeasurementScenario scenario, boolean visualize) {
		this.visualize = visualize;
		if (visualize) {
			visualizationOutputPath = "output_visualization/statechart_" + size;
		}

		storeBuilder = ModelStore.builder()
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

        var nodeCount = size * (size + 1);
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
                .seed(lane, builder ->
                        {
                            for (int i = 0; i < size * size; i++) {
                                builder.put(Tuple.of(i), TRUE);
                            }
							builder.reducedValue(UNKNOWN);
                        }
                )
                .seed(vehicle, builder ->
                        {
                            for (int i = size * size; i < nodeCount; i++) {
                                builder.put(Tuple.of(i), TRUE);
                            }
							builder.reducedValue(UNKNOWN);
                        }
                )
                .seed(followedBy, builder ->
                        {
                            for (int i = 0; i < size; i++) {
                                for (int j = 0; j < size; j++) {
                                    builder.put(Tuple.of(i * size + j, i * size + (j + 1) % size), TRUE);
                                }
                            }
							builder.reducedValue(FALSE);
                        }
                )
                .seed(right, builder ->
                        {
							builder.reducedValue(UNKNOWN);
                        }
                )
                .seed(left, builder ->
                        {
                            for (int i = 0; i < size; i++) {
                                for (int j = 0; j < size ; j++) {
									builder.put(Tuple.of(((i + 1) % size) * size + j, i * size + j), TRUE);
                                }
                            }
							builder.reducedValue(FALSE);
                        }
                )
                .seed(on, builder ->
                        {
                            for (int i = 0; i < size; i++) {
                                builder.put(Tuple.of(size * size + i, i * size), TRUE);
                            }
							builder.reducedValue(FALSE);
                        }
                )
                .seed(moveVehicleForwardPreconditionPartialRelation, builder -> builder.reducedValue(UNKNOWN))
                .seed(moveVehicleLeftPreconditionPartialRelation, builder -> builder.reducedValue(UNKNOWN))
                .seed(moveVehicleRightPreconditionPartialRelation, builder -> builder.reducedValue(UNKNOWN))
                .seed(removeVehiclePreconditionPartialRelation, builder -> builder.reducedValue(UNKNOWN))
                .seed(placeVehiclePreconditionPartialRelation, builder -> builder.reducedValue(UNKNOWN))
        ;

		this.modelSeed = this.modelSeedBuilder.build();
		super.init();
    }

	@Override
	protected boolean renderTransitionsToAlreadyVisitedStates() {
		return true;
	}
}

