package system;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import tools.refinery.logic.dnf.Query;
import tools.refinery.logic.dnf.RelationalQuery;
import tools.refinery.logic.term.CountTerm;
import tools.refinery.logic.term.Variable;
import tools.refinery.logic.term.cardinalityinterval.CardinalityIntervals;
import tools.refinery.logic.term.truthvalue.TruthValue;
import tools.refinery.store.dse.modification.ModificationAdapter;
import tools.refinery.store.dse.propagation.PropagationAdapter;
import tools.refinery.store.dse.strategy.BestFirstStoreManager;
import tools.refinery.store.dse.transition.DesignSpaceExplorationStoreAdapter;
import tools.refinery.store.dse.transition.Rule;
import tools.refinery.store.dse.transition.objectives.Criteria;
import tools.refinery.store.dse.transition.objectives.Criterion;
import tools.refinery.store.model.ModelStore;
import tools.refinery.store.query.ModelQueryAdapter;
import tools.refinery.store.query.interpreter.QueryInterpreterAdapter;
import tools.refinery.store.query.view.MustView;
import tools.refinery.store.reasoning.ReasoningAdapter;
import tools.refinery.store.reasoning.ReasoningStoreAdapter;
import tools.refinery.store.reasoning.representation.PartialRelation;
import tools.refinery.store.reasoning.seed.ModelSeed;
import tools.refinery.store.reasoning.translator.PartialRelationTranslator;
import tools.refinery.store.reasoning.translator.multiobject.MultiObjectTranslator;
import tools.refinery.store.representation.Symbol;
import tools.refinery.store.statecoding.StateCoderAdapter;
import tools.refinery.store.transition.system.simple.SimpleTransitionSystemAdapter;
import tools.refinery.store.tuple.Tuple;
import tools.refinery.visualization.ModelVisualizerAdapter;
import tools.refinery.visualization.internal.FileFormat;

import java.util.List;
import java.util.function.Consumer;

import static tools.refinery.logic.literal.Literals.check;
import static tools.refinery.logic.literal.Literals.not;
import static tools.refinery.logic.term.int_.IntTerms.constant;
import static tools.refinery.logic.term.int_.IntTerms.eq;
import static tools.refinery.store.dse.transition.actions.ActionLiterals.put;
import static tools.refinery.store.reasoning.ReasoningAdapter.EQUALS_SYMBOL;
import static tools.refinery.store.reasoning.actions.PartialActionLiterals.add;
import static tools.refinery.store.reasoning.literal.PartialLiterals.must;

public class SimplePartialTransitionSystemTest {
	PartialRelation person = new PartialRelation("Person", 1);
	PartialRelation friend = new PartialRelation("friend", 2);

	Symbol<TruthValue> personStorage = Symbol.of("Person", 1, TruthValue.class, TruthValue.FALSE);
	Symbol<TruthValue> friendStorage = Symbol.of("friend", 2, TruthValue.class, TruthValue.FALSE);

	RelationalQuery befriendPrecondition = Query.of("befriendPrecondition", (builder, p1, p2, f) -> builder
			.clause(
					person.call(p1),
					person.call(p2),
					not(EQUALS_SYMBOL.call(p1, p2)),
					not(friend.call(p1, p2)),
					friend.call(p1, f),
					friend.call(p2, f)
			)
	);
	Rule befriend = Rule.of("befriend", (builder, p1, p2, f) -> builder
			.clause(
					befriendPrecondition.call(p1, p2, f)
			)
			.action(
					put(friendStorage, TruthValue.TRUE, p1, p2),
					put(friendStorage, TruthValue.TRUE, p2, p1)
			));

	RelationalQuery estrangePrecondition = Query.of("estrangePrecondition", (builder, p1, p2) -> builder
			.clause(
					person.call(p1),
					person.call(p2),
					friend.call(p1, p2)
			)
	);
	Rule estrange = Rule.of("estrange", (builder, p1, p2) -> builder
			.clause(
					estrangePrecondition.call(p1, p2)
			)
			.action(
					put(friendStorage, TruthValue.FALSE, p1, p2),
					put(friendStorage, TruthValue.FALSE, p2, p1)
			)
	);

	RelationalQuery triumvirateQuery = Query.of("triumvirate", (builder, p1, p2, p3) -> builder
			.clause(
					must(person.call(p1)),
					must(person.call(p2)),
					must(person.call(p3)),
					must(friend.call(p1, p2)),
					must(friend.call(p1, p3)),
					must(friend.call(p2, p1)),
					must(friend.call(p2, p3)),
					must(friend.call(p3, p1)),
					must(friend.call(p3, p2)),
					check(eq(new CountTerm(new MustView(friendStorage), List.of(p1, Variable.of())), constant(2))),
					check(eq(new CountTerm(new MustView(friendStorage), List.of(p2, Variable.of())), constant(2))),
					check(eq(new CountTerm(new MustView(friendStorage), List.of(p3, Variable.of())), constant(2)))
			)
	);
	Criterion target = Criteria.whenHasMatch(triumvirateQuery);

	ModelStore store = ModelStore.builder()
			.with(QueryInterpreterAdapter.builder())
			.with(PropagationAdapter.builder()
					.rule(Rule.of("symmetricFriendship", (builder, p1, p2) -> builder
							.clause(
									must(friend.call(p1, p2)),
									not(must(friend.call(p2, p1)))
							)
							.action(
									add(friend, p2, p1)
							)))
			)
			.with(StateCoderAdapter.builder())
			.with(ModificationAdapter.builder())
			.with(SimpleTransitionSystemAdapter.builder()
							.transition(befriend)
							.transition(estrange)
							.accept(target)
//							.accept(model -> () -> false) // full state space exploration
			)
			.with(ModelVisualizerAdapter.builder()
					.withOutputPath("test_output")
					.withFormat(FileFormat.SVG)
					.saveStates()
					.saveDesignSpace()
			)
			.with(ReasoningAdapter.builder())
			.with(new MultiObjectTranslator())
			.with(PartialRelationTranslator.of(person)
					.symbol(personStorage)
			)
			.with(PartialRelationTranslator.of(friend)
					.symbol(friendStorage)
			)
			.build();

	ModelSeed.Builder seed = ModelSeed.builder(4)
			.seed(MultiObjectTranslator.COUNT_SYMBOL, builder -> builder
					.reducedValue(CardinalityIntervals.ONE)
			)
			.seed(person, builder -> builder
					.put(Tuple.of(0), TruthValue.TRUE)
					.put(Tuple.of(1), TruthValue.TRUE)
					.put(Tuple.of(2), TruthValue.TRUE)
					.put(Tuple.of(3), TruthValue.TRUE)
			);

	@Test
	void testTransitionSystemTargetReachable() {
		var seed = this.seed
				.seed(friend, builder -> builder
						.put(Tuple.of(0, 1), TruthValue.TRUE)
						.put(Tuple.of(1, 0), TruthValue.TRUE)
						.put(Tuple.of(0, 2), TruthValue.UNKNOWN)
						.put(Tuple.of(2, 0), TruthValue.UNKNOWN)
						.put(Tuple.of(2, 3), TruthValue.TRUE)
						.put(Tuple.of(3, 2), TruthValue.TRUE)
				)
				.build();
		performTest(seed, (bestFirst) -> {
			var solutions = bestFirst.getSolutionStore().getSolutions();
			Assertions.assertEquals(1, solutions.size());
		});
	}

	@Test
	void testTransitionSystemTargetUnreachable() {
		var seed = this.seed
				.seed(friend, builder -> builder
						.put(Tuple.of(0, 1), TruthValue.TRUE)
						.put(Tuple.of(1, 0), TruthValue.TRUE)
						.put(Tuple.of(2, 3), TruthValue.TRUE)
						.put(Tuple.of(3, 2), TruthValue.TRUE)
				)
				.build();
		performTest(seed, (bestFirst) -> {
			var solutions = bestFirst.getSolutionStore().getSolutions();
			Assertions.assertEquals(0, solutions.size());
		});
	}

	private void performTest(ModelSeed seed, Consumer<BestFirstStoreManager> check) {
		try (var model = store.getAdapter(ReasoningStoreAdapter.class).createInitialModel(seed)) {
			var queryEngine = model.getAdapter(ModelQueryAdapter.class);
			var initialVersion = model.commit();
			queryEngine.flushChanges();

			var bestFirst = new BestFirstStoreManager(store, 1);
			bestFirst.startExploration(initialVersion);
			model.getAdapter(ModelVisualizerAdapter.class).visualize(store.getAdapter(DesignSpaceExplorationStoreAdapter.class).getStateSpaceStore(), true);
			check.accept(bestFirst);
		}
	}

}
