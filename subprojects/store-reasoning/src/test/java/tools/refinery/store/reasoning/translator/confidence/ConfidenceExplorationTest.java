/*
 * SPDX-FileCopyrightText: 2023-2024 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.reasoning.translator.confidence;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import tools.refinery.logic.term.cardinalityinterval.CardinalityIntervals;
import tools.refinery.logic.term.truthvalue.TruthValue;
import tools.refinery.logic.term.truthvalue.TruthValueConfidence;
import tools.refinery.store.dse.propagation.PropagationAdapter;
import tools.refinery.store.dse.strategy.BestFirstStoreManager;
import tools.refinery.store.dse.transition.DesignSpaceExplorationAdapter;
import tools.refinery.store.model.Model;
import tools.refinery.store.model.ModelStore;
import tools.refinery.store.query.ModelQueryAdapter;
import tools.refinery.store.query.interpreter.QueryInterpreterAdapter;
import tools.refinery.store.reasoning.ReasoningAdapter;
import tools.refinery.store.reasoning.ReasoningStoreAdapter;
import tools.refinery.store.reasoning.representation.ConfidencePartialRelation;
import tools.refinery.store.reasoning.representation.PartialRelation;
import tools.refinery.store.reasoning.seed.ModelSeed;
import tools.refinery.store.reasoning.translator.containment.ContainmentHierarchyTranslator;
import tools.refinery.store.reasoning.translator.metamodel.ConfidenceMetamodel;
import tools.refinery.store.reasoning.translator.metamodel.ConfidenceMetamodelTranslator;
import tools.refinery.store.reasoning.translator.multiobject.MultiObjectTranslator;
import tools.refinery.store.statecoding.StateCoderAdapter;
import tools.refinery.store.tuple.Tuple;
import tools.refinery.visualization.ModelVisualizerAdapter;
import tools.refinery.visualization.internal.FileFormat;

class ConfidenceExplorationTest {

	private final PartialRelation road = new PartialRelation("road", 1);
	private final PartialRelation roadElement = new PartialRelation("roadElement", 1);
	private final PartialRelation lane = new PartialRelation("lane", 1);
	private final PartialRelation sidewalk = new PartialRelation("sidewalk", 1);
	private final PartialRelation actor = new PartialRelation("actor", 1);
	private final PartialRelation car = new PartialRelation("car", 1);
	private final PartialRelation pedestrian = new PartialRelation("pedestrian", 1);
	private final PartialRelation roadElements = new PartialRelation("roadElements", 2);
	private final PartialRelation position = new PartialRelation("position", 2);
	private final ConfidencePartialRelation positionConfidence = new ConfidencePartialRelation(
			"positionConfidence",	2);
	private final PartialRelation invalidRoadElementCount = new PartialRelation("invalidRoadElementCount", 1);
	private final PartialRelation invalidPositionCount = new PartialRelation("invalidPositionCount", 1);

	@Test
	@Disabled("This test is only for debugging purposes")
	void explorationWithConfidenceTest() {
		var metamodel = ConfidenceMetamodel.builder()
				.type(road)
				.type(roadElement, true)
				.type(lane, roadElement)
				.type(sidewalk, roadElement)
				.type(actor, true)
				.type(car, actor)
				.type(pedestrian, actor)
				.reference(roadElements, builder -> builder
						.containment(true)
						.source(road)
						.multiplicity(CardinalityIntervals.SOME, invalidRoadElementCount)
						.target(roadElement))
				.directedReference(positionConfidence, (builder -> builder
						.source(actor)
						.multiplicity(CardinalityIntervals.ONE, invalidPositionCount)
						.target(roadElement)
						.partialSymbol(position)))
				.build();

		var seed = ModelSeed.builder(12)
				.seed(MultiObjectTranslator.COUNT_SYMBOL, builder -> builder
						.reducedValue(CardinalityIntervals.ONE))
				.seed(ContainmentHierarchyTranslator.CONTAINER_SYMBOL, builder -> builder
						.reducedValue(TruthValue.UNKNOWN))
				.seed(ContainmentHierarchyTranslator.CONTAINED_SYMBOL, builder -> builder
						.reducedValue(TruthValue.UNKNOWN))
				.seed(ContainmentHierarchyTranslator.CONTAINS_SYMBOL, builder -> builder
						.reducedValue(TruthValue.UNKNOWN))
				.seed(road, builder -> builder.reducedValue(TruthValue.UNKNOWN)
						.put(Tuple.of(0), TruthValue.TRUE)
						.put(Tuple.of(1), TruthValue.TRUE))
				.seed(roadElement, builder -> builder.reducedValue(TruthValue.UNKNOWN))
				.seed(lane, builder -> builder.reducedValue(TruthValue.UNKNOWN)
						.put(Tuple.of(2), TruthValue.TRUE)
						.put(Tuple.of(3), TruthValue.TRUE)
						.put(Tuple.of(4), TruthValue.TRUE)
						.put(Tuple.of(5), TruthValue.TRUE))
				.seed(sidewalk, builder -> builder
						.reducedValue(TruthValue.UNKNOWN)
						.put(Tuple.of(6), TruthValue.TRUE)
						.put(Tuple.of(7), TruthValue.TRUE))
				.seed(actor, builder -> builder
						.reducedValue(TruthValue.UNKNOWN))
				.seed(car, builder -> builder.reducedValue(TruthValue.UNKNOWN)
						.put(Tuple.of(8), TruthValue.TRUE)
						.put(Tuple.of(9), TruthValue.TRUE)
						.put(Tuple.of(10), TruthValue.TRUE))
				.seed(pedestrian, builder -> builder
						.reducedValue(TruthValue.UNKNOWN)
						.put(Tuple.of(11), TruthValue.TRUE))
				.seed(roadElements, builder -> builder
						.reducedValue(TruthValue.UNKNOWN)
						.put(Tuple.of(0, 2), TruthValue.TRUE)
						.put(Tuple.of(0, 3), TruthValue.TRUE)
						.put(Tuple.of(0, 6), TruthValue.TRUE)
						.put(Tuple.of(1, 4), TruthValue.TRUE)
						.put(Tuple.of(1, 5), TruthValue.TRUE)
						.put(Tuple.of(1, 7), TruthValue.TRUE))
				.seed(positionConfidence, builder -> builder
						.reducedValue(TruthValueConfidence.FALSE)
						.put(Tuple.of(8, 2), new TruthValueConfidence(TruthValue.UNKNOWN, 0.98))
						.put(Tuple.of(8, 3), new TruthValueConfidence(TruthValue.UNKNOWN, 0.1))
						.put(Tuple.of(8, 4), new TruthValueConfidence(TruthValue.UNKNOWN, 0.013))
						.put(Tuple.of(8, 5), new TruthValueConfidence(TruthValue.UNKNOWN, 0.004))
						.put(Tuple.of(8, 6), new TruthValueConfidence(TruthValue.UNKNOWN, 0.001))
						.put(Tuple.of(8, 7), new TruthValueConfidence(TruthValue.UNKNOWN, 0.0002))
						.put(Tuple.of(9, 2), new TruthValueConfidence(TruthValue.UNKNOWN, 0.14))
						.put(Tuple.of(9, 3), new TruthValueConfidence(TruthValue.UNKNOWN, 0.78))
						.put(Tuple.of(9, 4), new TruthValueConfidence(TruthValue.UNKNOWN, 0.554))
						.put(Tuple.of(9, 5), new TruthValueConfidence(TruthValue.UNKNOWN, 0.017))
						.put(Tuple.of(9, 6), new TruthValueConfidence(TruthValue.UNKNOWN, 0.001))
						.put(Tuple.of(9, 7), new TruthValueConfidence(TruthValue.UNKNOWN, 0.0002))
						.put(Tuple.of(10, 2), new TruthValueConfidence(TruthValue.UNKNOWN, 0.0159))
						.put(Tuple.of(10, 3), new TruthValueConfidence(TruthValue.UNKNOWN, 0.30011))
						.put(Tuple.of(10, 4), new TruthValueConfidence(TruthValue.UNKNOWN, 0.71))
						.put(Tuple.of(10, 5), new TruthValueConfidence(TruthValue.UNKNOWN, 0.887))
						.put(Tuple.of(10, 6), new TruthValueConfidence(TruthValue.UNKNOWN, 0.001))
						.put(Tuple.of(10, 7), new TruthValueConfidence(TruthValue.UNKNOWN, 0.0002))
						.put(Tuple.of(11, 2), new TruthValueConfidence(TruthValue.UNKNOWN, 0.0754))
						.put(Tuple.of(11, 3), new TruthValueConfidence(TruthValue.UNKNOWN, 0.022))
						.put(Tuple.of(11, 4), new TruthValueConfidence(TruthValue.UNKNOWN, 0.1208))
						.put(Tuple.of(11, 5), new TruthValueConfidence(TruthValue.UNKNOWN, 0.0997))
						.put(Tuple.of(11, 6), new TruthValueConfidence(TruthValue.UNKNOWN, 0.84))
						.put(Tuple.of(11, 7), new TruthValueConfidence(TruthValue.UNKNOWN, 0.00121)))
				.build();

		var translator = new ConfidenceMetamodelTranslator(metamodel);

		var modelStore = createModelStore(translator);
		try (var model = createModel(modelStore, seed)) {
			var initialVersion = model.commit();
			var queryEngine = model.getAdapter(ModelQueryAdapter.class);
			queryEngine.flushChanges();
			var bestFirst = new BestFirstStoreManager(modelStore, 50);
			bestFirst.startExploration(initialVersion, 0, translator.getUpQuery());
			var resultStore = bestFirst.getSolutionStore();
			System.out.println("states size: " + resultStore.getSolutions().size());
			model.getAdapter(ModelVisualizerAdapter.class).visualize(bestFirst.getVisualizationStore());
		}

	}

	private static ModelStore createModelStore(ConfidenceMetamodelTranslator metamodelTranslator) {
		return ModelStore.builder()
				.with(QueryInterpreterAdapter.builder())
				.with(ModelVisualizerAdapter.builder()
						.withOutputPath("test_output")
						.withFormat(FileFormat.DOT)
						.withFormat(FileFormat.SVG)
						.saveStates()
						.saveDesignSpace())
				.with(PropagationAdapter.builder())
				.with(StateCoderAdapter.builder())
				.with(DesignSpaceExplorationAdapter.builder())
				.with(ReasoningAdapter.builder())
				.with(new MultiObjectTranslator())
				.with(metamodelTranslator)
				.build();
	}

	private static Model createModel(ModelStore store, ModelSeed seed) {
		return store.getAdapter(ReasoningStoreAdapter.class).createInitialModel(seed);
	}
}
