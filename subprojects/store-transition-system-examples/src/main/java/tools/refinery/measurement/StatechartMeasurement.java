package tools.refinery.measurement;

import tools.refinery.logic.dnf.Query;
import tools.refinery.logic.dnf.RelationalQuery;
import tools.refinery.logic.term.cardinalityinterval.CardinalityIntervals;
import tools.refinery.logic.term.truthvalue.TruthValue;
import tools.refinery.logic.term.truthvalue.TruthValueDomain;
import tools.refinery.logic.term.truthvalue.TruthValueMayTerm;
import tools.refinery.logic.term.truthvalue.TruthValueTerms;
import tools.refinery.store.dse.modification.ModificationAdapter;
import tools.refinery.store.dse.propagation.PropagationAdapter;
import tools.refinery.store.dse.transition.DesignSpaceExplorationAdapter;
import tools.refinery.store.dse.transition.Rule;
import tools.refinery.store.query.interpreter.QueryInterpreterAdapter;
import tools.refinery.store.reasoning.ReasoningAdapter;
import tools.refinery.store.reasoning.actions.PartialActionLiterals;
import tools.refinery.store.reasoning.interpretation.PartialNeighborhoodCalculator;
import tools.refinery.store.reasoning.literal.Concreteness;
import tools.refinery.store.reasoning.literal.CountLowerBoundLiteral;
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

import java.util.List;
import java.util.Set;

import static tools.refinery.logic.term.truthvalue.TruthValue.*;

public class StatechartMeasurement extends MeasurementBase {

    PartialRelation state = new PartialRelation("State", 1);
	PartialRelation initialState = new PartialRelation("InitialState", 1);
	PartialRelation transition = new PartialRelation("Transition", 1);
//	PartialFunction<TruthValue, Boolean> active = PartialSymbol.of("active", 1,
//			TruthValueDomain.INSTANCE);
    PartialRelation source = new PartialRelation("source", 2);
	PartialRelation sourceInvalidMultiplicity = new PartialRelation("source::invalidMultiplicity", 1);
    PartialRelation target = new PartialRelation("target", 2);
	PartialRelation targetInvalidMultiplicity = new PartialRelation("target::invalidMultiplicity", 1);

	PartialRelation active = new PartialRelation("active", 1);


	MetamodelBuilder builder = Metamodel.builder()
			.type(state)
			.type(initialState, state)
			.type(transition)
			.reference(source, referenceBuilder -> referenceBuilder
					.source(transition)
					.target(state)
					.multiplicity(CardinalityIntervals.ONE, sourceInvalidMultiplicity)
					.defaultValue(TruthValue.FALSE))
			.reference(target, referenceBuilder -> referenceBuilder
					.source(transition)
					.target(state)
					.multiplicity(CardinalityIntervals.LONE, targetInvalidMultiplicity)
					.defaultValue(TruthValue.FALSE));

	Metamodel metamodel = builder.build();


    PartialRelation stateTransitionPreconditionPartialRelation = new PartialRelation("stateTransition_precondition", 3);
    TransitionRule stateTransition = new TransitionRule(stateTransitionPreconditionPartialRelation,
            Rule.of("stateTransition", (builder, t, from, to) -> builder
            .clause(
                    transition.call(t),
					state.call(from),
					state.call(to),
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
		if (visualize) {
			visualizationOutputPath = "output_visualization/statechart_" + size;
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
						.transition(stateTransition)
						.accept(_ -> () -> false) // full state space exploration
				)
				.with(new MultiObjectTranslator())
				.with(new MetamodelTranslator(metamodel))
				.with(new PredicateTranslator(stateTransitionPreconditionPartialRelation, stateTransition.rule().getPrecondition(),
						List.of(transition, state, state), Set.of(), true, UNKNOWN))

				.with(new BasePredicateTranslator(active, List.of(state), Set.of(), UNKNOWN,
						new ConcretizationSettings(true, true)));

		int nodeIdCounter = 0;
		int redState = nodeIdCounter++;
		int redYellowState = nodeIdCounter++;
		int yellowState = nodeIdCounter++;
		int greenState = nodeIdCounter++;
		int t1 = nodeIdCounter++;
		int t2 = nodeIdCounter++;
		int t3 = nodeIdCounter++;
		int t4 = nodeIdCounter++;


        var nodeCount = nodeIdCounter;
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
                .seed(state, builder ->
                        {
							builder.put(Tuple.of(redState), TruthValue.TRUE);
							builder.put(Tuple.of(redYellowState), TruthValue.TRUE);
							builder.put(Tuple.of(yellowState), TruthValue.TRUE);
							builder.put(Tuple.of(greenState), TruthValue.TRUE);
							builder.reducedValue(UNKNOWN);
                        }
                )
				.seed(initialState, builder ->
						{
							builder.put(Tuple.of(redState), TruthValue.TRUE);
							builder.reducedValue(FALSE);
						}
				)
                .seed(transition, builder ->
                        {
							builder.put(Tuple.of(t1), TruthValue.TRUE);
							builder.put(Tuple.of(t2), TruthValue.TRUE);
							builder.put(Tuple.of(t3), TruthValue.TRUE);
							builder.put(Tuple.of(t4), TruthValue.TRUE);
							builder.reducedValue(UNKNOWN);
                        }
                )
                .seed(source, builder ->
                        {
							builder.put(Tuple.of(t1, redState), TruthValue.TRUE);
							builder.put(Tuple.of(t2, redYellowState), TruthValue.TRUE);
							builder.put(Tuple.of(t3, greenState), TruthValue.TRUE);
							builder.put(Tuple.of(t4, yellowState), TruthValue.TRUE);
							builder.reducedValue(TruthValue.FALSE);
                        }
                )
                .seed(target, builder ->
                        {
							builder.put(Tuple.of(t1, redYellowState), TruthValue.TRUE);
							builder.put(Tuple.of(t2, greenState), TruthValue.TRUE);
							builder.put(Tuple.of(t3, yellowState), TruthValue.TRUE);
							builder.put(Tuple.of(t4, redState), TruthValue.TRUE);
							builder.reducedValue(TruthValue.FALSE);
                        }
                )
				.seed(active, builder ->
						{
							builder.put(Tuple.of(redState), TruthValue.TRUE);
							builder.reducedValue(FALSE);
						}
				)
                .seed(stateTransitionPreconditionPartialRelation, builder -> builder.reducedValue(TruthValue.UNKNOWN))
        ;

//        System.out.println("building model seed with " + nodeCount + " nodes");
        this.modelSeed = this.modelSeedBuilder.build();
		super.init();
    }

	@Override
	protected boolean renderTransitionsToAlreadyVisitedStates() {
		return true;
	}
}

