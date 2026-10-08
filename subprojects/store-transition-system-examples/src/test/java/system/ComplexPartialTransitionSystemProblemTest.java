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
import tools.refinery.store.dse.propagation.PropagationAdapter;
import tools.refinery.store.dse.transition.DesignSpaceExplorationAdapter;
import tools.refinery.store.dse.transition.DesignSpaceExplorationStoreAdapter;
import tools.refinery.store.model.ModelStore;
import tools.refinery.store.query.interpreter.QueryInterpreterAdapter;
import tools.refinery.store.reasoning.ReasoningAdapter;
import tools.refinery.store.reasoning.ReasoningStoreAdapter;
import tools.refinery.store.statecoding.StateCoderAdapter;
import tools.refinery.store.transition.system.TransitionSystemAdapter;
import tools.refinery.store.transition.system.strategy.TransitionSystemStoreManager;
import tools.refinery.visualization.ModelVisualizerAdapter;
import tools.refinery.visualization.internal.FileFormat;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;

@InjectWithRefinery
@Disabled("For debugging purposes only")
class ComplexPartialTransitionSystemProblemTest {
	@Inject
	private ProblemParseHelper parseHelper;

	@Inject
	private ModelInitializer modelInitializer;

	@Test
	void transitionSystemProblemTest() {
		var parsedProblem = parseHelper.parse("""
				import builtin::strategy.

				class Person {
					Person[] friend
					Animal[] pet
				}

				class Animal.

				pred leader(p).

				pred friendly(p) <->
					Person(p),
					Person(p2),
					friend(p, p2).

				pred hasPet(p) <->
					Person(p),
					Animal(p2),
					pet(p, p2).

				pred happy(p) <->
					friendly(p),
					hasPet(p).

				@target
				pred anyLeader() <->
					Person(p),
					leader(p).

				transition rule becomesLeader(p) <->
					Person(p),
					happy(p),
					!anyLeader()
				==>
					leader(p).

				transition rule estrange(p1, p2) <->
					Person(p1),
					Person(p2),
					friend(p1, p2)
				==>
					!friend(p1, p2),
					!friend(p2, p1).

				transition rule lonelyMakesFriend(p1, p2) <->
					Person(p1),
					Person(p2),
					p1 != p2,
					!friendly(p1)
				==>
					friend(p1, p2),
					friend(p2, p1).

				propagation rule symmetricFriendship(p1, p2) <->
					must friend(p1, p2),
					!must friend(p2, p1)
				==>
					friend(p2, p1).

				Person(Bela).
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
						.withOutputPath("test_output")
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
		var test = injector.getInstance(ComplexPartialTransitionSystemProblemTest.class);
		try {
			test.transitionSystemProblemTest();
		} catch (Throwable e) {
			e.printStackTrace();
		}
	}
}
