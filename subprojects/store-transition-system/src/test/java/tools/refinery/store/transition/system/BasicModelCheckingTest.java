/*
 * SPDX-FileCopyrightText: 2026 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package system;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import tools.refinery.logic.dnf.Query;
import tools.refinery.logic.dnf.RelationalQuery;
import tools.refinery.logic.term.cardinalityinterval.CardinalityIntervals;
import tools.refinery.logic.term.truthvalue.TruthValue;
import tools.refinery.store.dse.modification.ModificationAdapter;
import tools.refinery.store.dse.propagation.PropagationAdapter;
import tools.refinery.store.dse.transition.DesignSpaceExplorationAdapter;
import tools.refinery.store.dse.transition.DesignSpaceExplorationStoreAdapter;
import tools.refinery.store.dse.transition.Rule;
import tools.refinery.store.dse.transition.objectives.Criterion;
import tools.refinery.store.model.ModelStore;
import tools.refinery.store.query.ModelQueryAdapter;
import tools.refinery.store.query.interpreter.QueryInterpreterAdapter;
import tools.refinery.store.reasoning.ReasoningAdapter;
import tools.refinery.store.reasoning.ReasoningStoreAdapter;
import tools.refinery.store.reasoning.interpretation.PartialInterpretation;
import tools.refinery.store.reasoning.interpretation.wrapper.PartialInterpretationInterpretationWrapper;
import tools.refinery.store.reasoning.literal.Concreteness;
import tools.refinery.store.reasoning.representation.PartialRelation;
import tools.refinery.store.reasoning.representation.wrapper.PartialSymbolSymbolWrapper;
import tools.refinery.store.reasoning.seed.ModelSeed;
import tools.refinery.store.reasoning.translator.ConcretizationSettings;
import tools.refinery.store.reasoning.translator.PartialRelationTranslator;
import tools.refinery.store.reasoning.translator.multiobject.MultiObjectTranslator;
import tools.refinery.store.reasoning.translator.predicate.BasePredicateTranslator;
import tools.refinery.store.reasoning.translator.predicate.PredicateTranslator;
import tools.refinery.store.representation.Symbol;
import tools.refinery.store.statecoding.StateCoderAdapter;
import tools.refinery.store.transition.system.TransitionSystemAdapter;
import tools.refinery.store.transition.system.statespace.TransitionRule;
import tools.refinery.store.transition.system.strategy.TransitionSystemStoreManager;
import tools.refinery.store.tuple.Tuple;
import tools.refinery.visualization.ModelVisualizerAdapter;
import tools.refinery.visualization.internal.FileFormat;
import tools.refinery.store.model.wrapper.InterpretationWrapper;
import tools.refinery.store.representation.wrapper.SymbolWrapper;

import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.IntStream;

import static tools.refinery.logic.literal.Literals.not;
import static tools.refinery.logic.term.truthvalue.TruthValue.FALSE;
import static tools.refinery.logic.term.truthvalue.TruthValue.TRUE;
import static tools.refinery.logic.term.truthvalue.TruthValue.UNKNOWN;
import static tools.refinery.store.reasoning.actions.PartialActionLiterals.modify;

public class BasicModelCheckingTest {

	/*
	 * ------ Program ------
	 * x, y, z: Boolean
	 * if x v y
	 *   then
	 *     if !y
	 *       then assert x  # pass
	 * 	     else assert y  # pass
	 *     z := false
	 *   else z := true
	 * assert x v y == !z  # pass
	 * x := havoc()
	 * assert x v y == !z  # fail
	 *
	 * ------- CFA -------
	 *                       (L0)
	 *                        |
	 *             +----------+---------+
	 *             |                    |
	 *         [x∨y=TRUE]          [x∨y=FALSE]
	 *            (L1)                  |
	 *             |                    |
	 *     +-------+-------+            |
	 *     |               |            |
	 * [¬y=TRUE]      [¬y=FALSE]        |
	 *    (L2)            (L3)          |
	 *     |               |            |
	 *     | assert x      | assert y   |
	 *     +------+        +-----+      |
	 *     |      |        |     |      |
	 *  [pass] [fail]   [pass] [fail]   |
	 *     |    (LE1)      |   (LE2)    |
	 *     |               |            |
	 *     +-------+-------+            |
	 *             |                    |
	 *            (L4)                  |
	 *             | z := false         | z := true
	 *             |                    |
	 *             +--------+-----------+
	 *                      |
	 *                     (L5)
	 *                      |
	 *                      | assert x ∨ y == ¬z
	 *                      |
	 *                 +----+----+
	 *                 |         |
	 *               [pass]    [fail]
	 *    x := havoc() |       (LE3)
	 *                 |
	 *                (L6)
	 *                 |
	 *                 | assert x ∨ y == ¬z
	 *                 |
	 *            +----+----+
	 *            |         |
	 *         [pass]     [fail]
	 *           (L7)     (LE4)
	 */

	int nodeIdCounter = 0;
	int env = nodeIdCounter++;
	int L0 = nodeIdCounter++;
	int L1 = nodeIdCounter++;
	int L2 = nodeIdCounter++;
	int L3 = nodeIdCounter++;
	int L4 = nodeIdCounter++;
	int L5 = nodeIdCounter++;
	int L6 = nodeIdCounter++;
	int L7 = nodeIdCounter++;
	int LE1 = nodeIdCounter++;
	int LE2 = nodeIdCounter++;
	int LE3 = nodeIdCounter++;
	int LE4 = nodeIdCounter++;

	PartialRelation environment = new PartialRelation("Environment", 1);
	PartialRelation location = new PartialRelation("Location", 1);
	PartialRelation loc = new PartialRelation("loc", 2);
	PartialRelation x = new PartialRelation("x", 1);
	PartialRelation y = new PartialRelation("y", 1);
	PartialRelation z = new PartialRelation("z", 1);

	Symbol<TruthValue> environmentStorage = Symbol.of("Environment", 1, TruthValue.class, FALSE);
	Symbol<TruthValue> locationStorage = Symbol.of("Location", 1, TruthValue.class, FALSE);

	PartialRelation x_v_y = new PartialRelation("x_v_y", 1);
	RelationalQuery x_v_y_query = Query.of("x_v_y", (builder, env) -> builder
			.clause(x.call(env))
			.clause(y.call(env))
	);

	PartialRelation x_v_y_equivalent_with_not_z = new PartialRelation("x_v_y_equivalent_with_not_z", 1);
	RelationalQuery x_v_y_equivalent_with_not_z_query = Query.of("x_v_y_equivalent_with_not_z", (builder, env) -> builder
			.clause(x_v_y.call(env), not(z.call(env)))
			.clause(not(x_v_y.call(env)), z.call(env))
	);

	PartialRelation L0_L1_relation = new PartialRelation("L0_L1_relation", 3);
	TransitionRule L0_L1 = new TransitionRule(
			L0_L1_relation,
			Rule.of("L0_L1", (builder, env, L0, L1) -> builder
					.clause(
							env.isConstant(this.env),
							L0.isConstant(this.L0),
							L1.isConstant(this.L1),
							loc.call(env, L0),
							x_v_y.call(env)
					)
					.action(
							modify(loc, FALSE, env, L0),
							modify(loc, TRUE, env, L1)
					)
			)
	);

	PartialRelation L1_L2_relation = new PartialRelation("L1_L2_relation", 3);
	TransitionRule L1_L2 = new TransitionRule(
			L1_L2_relation,
			Rule.of("L1_L2", (builder, env, L1, L2) -> builder
					.clause(
							env.isConstant(this.env),
							L1.isConstant(this.L1),
							L2.isConstant(this.L2),
							loc.call(env, L1),
							not(y.call(env))
					)
					.action(
							modify(loc, FALSE, env, L1),
							modify(loc, TRUE, env, L2)
					)
			)
	);

	PartialRelation L1_L3_relation = new PartialRelation("L1_L3_relation", 3);
	TransitionRule L1_L3 = new TransitionRule(
			L1_L3_relation,
			Rule.of("L1_L3", (builder, env, L1, L3) -> builder
					.clause(
							env.isConstant(this.env),
							L1.isConstant(this.L1),
							L3.isConstant(this.L3),
							loc.call(env, L1),
							y.call(env)
					)
					.action(
							modify(loc, FALSE, env, L1),
							modify(loc, TRUE, env, L3)
					)
			)
	);

	PartialRelation L2_L4_relation = new PartialRelation("L2_L4_relation", 3);
	TransitionRule L2_L4 = new TransitionRule(
			L2_L4_relation,
			Rule.of("L2_L4", (builder, env, L2, L4) -> builder
					.clause(
							env.isConstant(this.env),
							L2.isConstant(this.L2),
							L4.isConstant(this.L4),
							loc.call(env, L2),
							x.call(env)
					)
					.action(
							modify(loc, FALSE, env, L2),
							modify(loc, TRUE, env, L4)
					)
			)
	);

	PartialRelation L2_LE1_relation = new PartialRelation("L2_LE1_relation", 3);
	TransitionRule L2_LE1 = new TransitionRule(
			L2_LE1_relation,
			Rule.of("L2_LE1", (builder, env, L2, LE1) -> builder
					.clause(
							env.isConstant(this.env),
							L2.isConstant(this.L2),
							LE1.isConstant(this.LE1),
							loc.call(env, L2),
							not(x.call(env))
					)
					.action(
							modify(loc, FALSE, env, L2),
							modify(loc, TRUE, env, LE1)
					)
			)
	);

	PartialRelation L3_L4_relation = new PartialRelation("L3_L4_relation", 3);
	TransitionRule L3_L4 = new TransitionRule(
			L3_L4_relation,
			Rule.of("L3_L4", (builder, env, L3, L4) -> builder
					.clause(
							env.isConstant(this.env),
							L3.isConstant(this.L3),
							L4.isConstant(this.L4),
							loc.call(env, L3),
							y.call(env)
					)
					.action(
							modify(loc, FALSE, env, L3),
							modify(loc, TRUE, env, L4)
					)
			)
	);

	PartialRelation L3_LE2_relation = new PartialRelation("L3_LE2_relation", 3);
	TransitionRule L3_LE2 = new TransitionRule(
			L3_LE2_relation,
			Rule.of("L3_LE2", (builder, env, L3, LE2) -> builder
					.clause(
							env.isConstant(this.env),
							L3.isConstant(this.L3),
							LE2.isConstant(this.LE2),
							loc.call(env, L3),
							not(y.call(env))
					)
					.action(
							modify(loc, FALSE, env, L3),
							modify(loc, TRUE, env, LE2)
					)
			)
	);

	PartialRelation L4_L5_relation = new PartialRelation("L4_L5_relation", 3);
	TransitionRule L4_L5 = new TransitionRule(
			L4_L5_relation,
			Rule.of("L4_L5", (builder, env, L4, L5) -> builder
					.clause(
							env.isConstant(this.env),
							L4.isConstant(this.L4),
							L5.isConstant(this.L5),
							loc.call(env, L4)
					)
					.action(
							modify(loc, FALSE, env, L4),
							modify(loc, TRUE, env, L5),
							modify(z, FALSE, env)
					)
			)
	);

	PartialRelation L0_L5_relation = new PartialRelation("L0_L5_relation", 3);
	TransitionRule L0_L5 = new TransitionRule(
			L0_L5_relation,
			Rule.of("L0_L5", (builder, env, L0, L5) -> builder
					.clause(
							env.isConstant(this.env),
							L0.isConstant(this.L0),
							L5.isConstant(this.L5),
							loc.call(env, L0),
							not(x_v_y.call(env))
					)
					.action(
							modify(loc, FALSE, env, L0),
							modify(loc, TRUE, env, L5),
							modify(z, TRUE, env)
					)
			)
	);

	PartialRelation L5_L6_relation = new PartialRelation("L5_L6_relation", 3);
	TransitionRule L5_L6 = new TransitionRule(
			L5_L6_relation,
			Rule.of("L5_L6", (builder, env, L5, L6) -> builder
					.clause(
							env.isConstant(this.env),
							L5.isConstant(this.L5),
							L6.isConstant(this.L6),
							loc.call(env, L5),
							x_v_y_equivalent_with_not_z.call(env)
					)
					.action(
							modify(loc, FALSE, env, L5),
							modify(loc, TRUE, env, L6),
							modify(x, UNKNOWN, env)
					)
			)
	);

	PartialRelation L5_LE3_relation = new PartialRelation("L5_LE3_relation", 3);
	TransitionRule L5_LE3 = new TransitionRule(
			L5_LE3_relation,
			Rule.of("L5_LE3", (builder, env, L5, LE3) -> builder
					.clause(
							env.isConstant(this.env),
							L5.isConstant(this.L5),
							LE3.isConstant(this.LE3),
							loc.call(env, L5),
							not(x_v_y_equivalent_with_not_z.call(env))
					)
					.action(
							modify(loc, FALSE, env, L5),
							modify(loc, TRUE, env, LE3)
					)
			)
	);

	PartialRelation L6_L7_relation = new PartialRelation("L6_L7_relation", 3);
	TransitionRule L6_L7 = new TransitionRule(
			L6_L7_relation,
			Rule.of("L6_L7", (builder, env, L6, L7) -> builder
					.clause(
							env.isConstant(this.env),
							L6.isConstant(this.L6),
							L7.isConstant(this.L7),
							loc.call(env, L6),
							x_v_y_equivalent_with_not_z.call(env)
					)
					.action(
							modify(loc, FALSE, env, L6),
							modify(loc, TRUE, env, L7)
					)
			)
	);

	PartialRelation L6_LE4_relation = new PartialRelation("L6_LE4_relation", 3);
	TransitionRule L6_LE4 = new TransitionRule(
			L6_LE4_relation,
			Rule.of("L6_LE4", (builder, env, L6, LE4) -> builder
					.clause(
							env.isConstant(this.env),
							L6.isConstant(this.L6),
							LE4.isConstant(this.LE4),
							loc.call(env, L6),
							not(x_v_y_equivalent_with_not_z.call(env))
					)
					.action(
							modify(loc, FALSE, env, L6),
							modify(loc, TRUE, env, LE4)
					)
			)
	);

	BiFunction<Criterion, String, ModelStore> store = (criterion, outputDirectory) -> ModelStore.builder()
			.with(QueryInterpreterAdapter.builder())
			.with(PropagationAdapter.builder())
			.with(StateCoderAdapter.builder()
					.individuals(IntStream.range(0, nodeIdCounter).mapToObj(Tuple::of).toList())
			)
			.with(ModificationAdapter.builder())
			.with(DesignSpaceExplorationAdapter.builder())
			.with(ReasoningAdapter.builder())
			.with(TransitionSystemAdapter.builder()
					.transition(L0_L1)
					.transition(L1_L2)
					.transition(L1_L3)
					.transition(L2_L4)
					.transition(L2_LE1)
					.transition(L3_L4)
					.transition(L3_LE2)
					.transition(L0_L5)
					.transition(L4_L5)
					.transition(L5_L6)
					.transition(L5_LE3)
					.transition(L6_L7)
					.transition(L6_LE4)
					.accept(criterion)
			)
			.with(ModelVisualizerAdapter.builder()
					.withOutputPath("test_output/basic_model_checking_test_new/" + outputDirectory)
					.withFormat(FileFormat.SVG)
					.saveStates()
					.saveDesignSpace()
			)
			.with(new MultiObjectTranslator())
			.with(PartialRelationTranslator.of(environment).symbol(environmentStorage))
			.with(PartialRelationTranslator.of(location).symbol(locationStorage))
			.with(new BasePredicateTranslator(loc, List.of(environment, location), Set.of(), FALSE,
					new ConcretizationSettings(true, true)))
			.with(new BasePredicateTranslator(x, List.of(environment), Set.of(), FALSE,
					new ConcretizationSettings(true, true)))
			.with(new BasePredicateTranslator(y, List.of(environment), Set.of(), FALSE,
					new ConcretizationSettings(true, true)))
			.with(new BasePredicateTranslator(z, List.of(environment), Set.of(), FALSE,
					new ConcretizationSettings(true, true)))
			.with(new PredicateTranslator(x_v_y, x_v_y_query, List.of(environment), Set.of(), true, FALSE))
			.with(new PredicateTranslator(x_v_y_equivalent_with_not_z, x_v_y_equivalent_with_not_z_query, List.of(environment), Set.of(), true, FALSE))
			.with(new PredicateTranslator(L0_L1_relation, L0_L1.rule().getPrecondition(),
					Collections.nCopies(L0_L1_relation.arity(), null),
					Set.of(), true, UNKNOWN))
			.with(new PredicateTranslator(L1_L2_relation, L1_L2.rule().getPrecondition(),
					Collections.nCopies(L1_L2_relation.arity(), null),
					Set.of(), true, UNKNOWN))
			.with(new PredicateTranslator(L1_L3_relation, L1_L3.rule().getPrecondition(),
					Collections.nCopies(L1_L3_relation.arity(), null),
					Set.of(), true, UNKNOWN))
			.with(new PredicateTranslator(L2_L4_relation, L2_L4.rule().getPrecondition(),
					Collections.nCopies(L2_L4_relation.arity(), null),
					Set.of(), true, UNKNOWN))
			.with(new PredicateTranslator(L2_LE1_relation, L2_LE1.rule().getPrecondition(),
					Collections.nCopies(L2_LE1_relation.arity(), null),
					Set.of(), true, UNKNOWN))
			.with(new PredicateTranslator(L3_L4_relation, L3_L4.rule().getPrecondition(),
					Collections.nCopies(L3_L4_relation.arity(), null),
					Set.of(), true, UNKNOWN))
			.with(new PredicateTranslator(L3_LE2_relation, L3_LE2.rule().getPrecondition(),
					Collections.nCopies(L3_LE2_relation.arity(), null),
					Set.of(), true, UNKNOWN))
			.with(new PredicateTranslator(L0_L5_relation, L0_L5.rule().getPrecondition(),
					Collections.nCopies(L0_L5_relation.arity(), null),
					Set.of(), true, UNKNOWN))
			.with(new PredicateTranslator(L4_L5_relation, L4_L5.rule().getPrecondition(),
					Collections.nCopies(L4_L5_relation.arity(), null),
					Set.of(), true, UNKNOWN))
			.with(new PredicateTranslator(L5_L6_relation, L5_L6.rule().getPrecondition(),
					Collections.nCopies(L5_L6_relation.arity(), null),
					Set.of(), true, UNKNOWN))
			.with(new PredicateTranslator(L5_LE3_relation, L5_LE3.rule().getPrecondition(),
					Collections.nCopies(L5_LE3_relation.arity(), null),
					Set.of(), true, UNKNOWN))
			.with(new PredicateTranslator(L6_L7_relation, L6_L7.rule().getPrecondition(),
					Collections.nCopies(L6_L7_relation.arity(), null),
					Set.of(), true, UNKNOWN))
			.with(new PredicateTranslator(L6_LE4_relation, L6_LE4.rule().getPrecondition(),
					Collections.nCopies(L6_LE4_relation.arity(), null),
					Set.of(), true, UNKNOWN))
			.build();

	ModelSeed seed = ModelSeed.builder(nodeIdCounter)
			.seed(MultiObjectTranslator.COUNT_SYMBOL, builder -> builder
					.reducedValue(CardinalityIntervals.ONE)
			)
			.seed(environment, builder -> builder
					.put(Tuple.of(this.env), TruthValue.TRUE)
			)
			.seed(location, builder -> {
				for (int i = 0; i < nodeIdCounter; i++) {
					if (i != env) {
						builder.put(Tuple.of(i), TruthValue.TRUE);
					}
				}
			})
			.seed(loc, builder -> builder
					.put(Tuple.of(this.env, L0), TruthValue.TRUE)
			)
			.seed(x, builder -> builder
					.put(Tuple.of(this.env), UNKNOWN))
			.seed(y, builder -> builder
					.put(Tuple.of(this.env), UNKNOWN))
			.seed(z, builder -> builder
					.put(Tuple.of(this.env), UNKNOWN))
			.seed(x_v_y, builder -> builder
					.put(Tuple.of(this.env), UNKNOWN))
			.seed(x_v_y_equivalent_with_not_z, builder -> builder
					.put(Tuple.of(this.env), UNKNOWN))
			.seed(L0_L1_relation, builder -> builder
					.reducedValue(TruthValue.UNKNOWN))
			.seed(L1_L2_relation, builder -> builder
					.reducedValue(TruthValue.UNKNOWN))
			.seed(L1_L3_relation, builder -> builder
					.reducedValue(TruthValue.UNKNOWN))
			.seed(L2_L4_relation, builder -> builder
					.reducedValue(TruthValue.UNKNOWN))
			.seed(L2_LE1_relation, builder -> builder
					.reducedValue(TruthValue.UNKNOWN))
			.seed(L3_L4_relation, builder -> builder
					.reducedValue(TruthValue.UNKNOWN))
			.seed(L3_LE2_relation, builder -> builder
					.reducedValue(TruthValue.UNKNOWN))
			.seed(L0_L5_relation, builder -> builder
					.reducedValue(TruthValue.UNKNOWN))
			.seed(L4_L5_relation, builder -> builder
					.reducedValue(TruthValue.UNKNOWN))
			.seed(L5_L6_relation, builder -> builder
					.reducedValue(TruthValue.UNKNOWN))
			.seed(L5_LE3_relation, builder -> builder
					.reducedValue(TruthValue.UNKNOWN))
			.seed(L6_L7_relation, builder -> builder
					.reducedValue(TruthValue.UNKNOWN))
			.seed(L6_LE4_relation, builder -> builder
					.reducedValue(TruthValue.UNKNOWN))
			.build();

	@Test
	void testFullTransitionSystemExploration() {
		Criterion fullExploration = _ -> () -> false;
		performTest(fullExploration, "", (manager) -> {
			var solution = manager.getSolution();
			Assertions.assertNull(solution);
		});
	}

	@Test
	void testReachableTargetStates() {
		var expectedErrorReachability = Map.of(
				LE1, false,
				LE2, false,
				LE3, false,
				LE4, true
		);

		for (var entry : expectedErrorReachability.entrySet()) {
			var target = entry.getKey();
			var shouldBeReachable = entry.getValue();
			Criterion reachTargetStates = model -> {
				var locInterpretation =
						model.getAdapter(ReasoningAdapter.class).getPartialInterpretation(Concreteness.PARTIAL, loc);
				return () -> locInterpretation.get(Tuple.of(env, target)) == TRUE;
			};
			performTest(reachTargetStates, target.toString(), (manager) -> {
				var solution = manager.getSolution();
				if (shouldBeReachable) {
					Assertions.assertNotNull(solution);
					Assertions.assertEquals(solution.states().size(), solution.transitions().size() + 1);
				} else {
					Assertions.assertNull(solution);
				}
			});
		}
	}

	private void performTest(Criterion criterion, String outputDirectory,
	                         Consumer<TransitionSystemStoreManager> check) {
		var store = this.store.apply(criterion, outputDirectory);
		try (var model = store.getAdapter(ReasoningStoreAdapter.class).createInitialModel(seed)) {
			var queryEngine = model.getAdapter(ModelQueryAdapter.class);
			var initialVersion = model.commit();
			queryEngine.flushChanges();

			var manager = new TransitionSystemStoreManager(store);
			manager.startExploration(initialVersion);
			var visualizerAdapter = model.getAdapter(ModelVisualizerAdapter.class);
			var stateSpaceStore = store.getAdapter(DesignSpaceExplorationStoreAdapter.class).getStateSpaceStore();
			visualizerAdapter.visualize(stateSpaceStore, true);
			if (manager.getSolution() != null) {
//				visualizerAdapter.visualize(manager.getSolutionStateSpaceStore(), "trace/symbol", "trace");

				Function<Concreteness, Map<SymbolWrapper, InterpretationWrapper<?>>> getPartialInterpretations =
						(Concreteness concreteness) -> {
							Map<SymbolWrapper, InterpretationWrapper<?>> interpretations = new HashMap<>();
							var partialSymbols = model.getStore().getAdapter(ReasoningStoreAdapter.class).getPartialSymbols();
							var reasoningAdapter = model.getAdapter(ReasoningAdapter.class);
							for (var partialSymbol : partialSymbols) {
								var partialInterpretation =
										(PartialInterpretation<?, ?>) reasoningAdapter.getPartialInterpretation(concreteness, partialSymbol);
								interpretations.put(new PartialSymbolSymbolWrapper(partialSymbol),
										new PartialInterpretationInterpretationWrapper<>(partialInterpretation));
							}
							return interpretations;
						};

//				model.getAdapter(ModelVisualizerAdapter.class).visualize(store.getAdapter(DesignSpaceExplorationStoreAdapter.class).getStateSpaceStore(),
//						"trace/partial", "trace", getPartialInterpretations.apply(Concreteness.PARTIAL));
//				model.getAdapter(ModelVisualizerAdapter.class).visualize(store.getAdapter(DesignSpaceExplorationStoreAdapter.class).getStateSpaceStore(),
//						"trace/candidate", "trace",
//						getPartialInterpretations.apply(Concreteness.CANDIDATE));
			}
			check.accept(manager);
		}
	}
}
