/*
 * SPDX-FileCopyrightText: 2026 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package system;

import com.google.inject.Inject;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import tools.refinery.language.ProblemStandaloneSetup;
import tools.refinery.language.semantics.ModelInitializer;
import tools.refinery.language.tests.InjectWithRefinery;
import tools.refinery.language.tests.utils.ProblemParseHelper;
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
import tools.refinery.store.model.wrapper.InterpretationWrapper;
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
import tools.refinery.store.representation.wrapper.SymbolWrapper;
import tools.refinery.store.statecoding.StateCoderAdapter;
import tools.refinery.store.transition.system.TransitionSystemAdapter;
import tools.refinery.store.transition.system.statespace.TransitionRule;
import tools.refinery.store.transition.system.strategy.TransitionSystemStoreManager;
import tools.refinery.store.tuple.Tuple;
import tools.refinery.visualization.ModelVisualizerAdapter;
import tools.refinery.visualization.internal.FileFormat;

import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.IntStream;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static tools.refinery.logic.literal.Literals.not;
import static tools.refinery.logic.term.truthvalue.TruthValue.*;
import static tools.refinery.store.reasoning.actions.PartialActionLiterals.modify;

@InjectWithRefinery
@Disabled("For debugging purposes only")
public class BasicModelCheckingProblemTest {
	@Inject
	private ProblemParseHelper parseHelper;

	@Inject
	private ModelInitializer modelInitializer;

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


	@Test
	void testFullTransitionSystemExploration() {
		var parsedProblem = parseHelper.parse("""
				import builtin::strategy.

				class Environment {
					Location loc
				}

				class Location.

				pred x(Environment env).
				pred y(Environment env).
				pred z(Environment env).

				pred x_v_y(Environment env) <->
					x(env)
				;
					y(env).

				pred x_v_y_equivalent_with_not_z(Environment env) <->
					x_v_y(env),
					!z(env)
				;
					!x_v_y(env),
					z(env).

				transition rule L0_L1() <->
					loc(env, L0),
					x_v_y(env)
				==>
					!loc(env, L0),
					loc(env, L1).

				transition rule L1_L2() <->
					loc(env, L1),
					!y(env)
				==>
					!loc(env, L1),
					loc(env, L2).

				transition rule L1_L3() <->
					loc(env, L1),
					y(env)
				==>
					!loc(env, L1),
					loc(env, L3).

				transition rule L2_L4() <->
					loc(env, L2),
					x(env)
				==>
					!loc(env, L2),
					loc(env, L4).

				transition rule L2_LE1() <->
					loc(env, L2),
					!x(env)
				==>
					!loc(env, L2),
					loc(env, LE1).

				transition rule L3_L4() <->
					loc(env, L3),
					y(env)
				==>
					!loc(env, L3),
					loc(env, L4).

				transition rule L3_LE2() <->
					loc(env, L3),
					!y(env)
				==>
					!loc(env, L3),
					loc(env, LE2).

				transition rule L4_L5() <->
					loc(env, L4)
				==>
					!loc(env, L4),
					loc(env, L5),
					!z(env).

				transition rule L0_L5() <->
					loc(env, L0),
					!x_v_y(env)
				==>
					!loc(env, L0),
					loc(env, L5),
					z(env).

				transition rule L5_L6() <->
					loc(env, L5),
					x_v_y_equivalent_with_not_z(env)
				==>
					!loc(env, L5),
					loc(env, L6),
					?x(env).

				transition rule L5_LE3() <->
					loc(env, L5),
					!x_v_y_equivalent_with_not_z(env)
				==>
					!loc(env, L5),
					loc(env, LE3).

				transition rule L6_L7() <->
					loc(env, L6),
					x_v_y_equivalent_with_not_z(env)
				==>
					!loc(env, L6),
					loc(env, L7).

				transition rule L6_LE4() <->
					loc(env, L6),
					!x_v_y_equivalent_with_not_z(env)
				==>
					!loc(env, L6),
					loc(env, LE4).


				default !Environment(*).
				Environment(env).
				!exists(Environment::new).

				atom env.
				atom L0.
				atom L1.
				atom L2.
				atom L3.
				atom L4.
				atom L5.
				atom L6.
				atom L7.
				atom LE1.
				atom LE2.
				atom LE3.
				atom LE4.

				default !Location(*).
				Location(L0).
				Location(L1).
				Location(L2).
				Location(L3).
				Location(L4).
				Location(L5).
				Location(L6).
				Location(L7).
				Location(LE1).
				Location(LE2).
				Location(LE3).
				Location(LE4).
				!exists(Location::new).

				loc(env, L0).

				?x(env).
				?y(env).
				?z(env).


				@target
				pred accept() <->
					false.
				""");
		assertThat(parsedProblem.getResourceErrors(), empty());
		var problem = parsedProblem.problem();

		var storeBuilder = ModelStore.builder()
				.with(QueryInterpreterAdapter.builder())
				.with(PropagationAdapter.builder())
				.with(StateCoderAdapter.builder())
				.with(DesignSpaceExplorationAdapter.builder())
				.with(TransitionSystemAdapter.builder())
				.with(ModelVisualizerAdapter.builder()
						.withOutputPath("test_output/basic_model_checking_problem_test_new/")
						.withFormat(FileFormat.DOT)
						.withFormat(FileFormat.SVG)
						.saveStates()
						.saveDesignSpace())
				.with(ReasoningAdapter.builder());

		var modelSeed = modelInitializer.createModel(problem, storeBuilder);

		var store = storeBuilder.build();

		try (var model = store.getAdapter(ReasoningStoreAdapter.class).createInitialModel(modelSeed)) {

			var initialVersion = model.commit();

			var manager = new TransitionSystemStoreManager(store);
			manager.startExploration(initialVersion);
			model.getAdapter(ModelVisualizerAdapter.class).visualize(store.getAdapter(DesignSpaceExplorationStoreAdapter.class).getStateSpaceStore(), true);
			var solution = manager.getSolution();
			Assertions.assertNull(solution);
		}
	}

	public static void main(String[] args) {
		ProblemStandaloneSetup.doSetup();
		var injector = new ProblemStandaloneSetup().createInjectorAndDoEMFRegistration();
		var test = injector.getInstance(BasicModelCheckingProblemTest.class);
		try {
			test.testFullTransitionSystemExploration();
		} catch (Throwable e) {
			e.printStackTrace();
		}
	}
}
