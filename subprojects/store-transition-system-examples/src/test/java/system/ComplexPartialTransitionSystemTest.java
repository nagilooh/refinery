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
import tools.refinery.store.model.ModelStore;
import tools.refinery.store.query.ModelQueryAdapter;
import tools.refinery.store.query.interpreter.QueryInterpreterAdapter;
import tools.refinery.store.reasoning.ReasoningAdapter;
import tools.refinery.store.reasoning.ReasoningStoreAdapter;
import tools.refinery.store.reasoning.representation.PartialRelation;
import tools.refinery.store.reasoning.seed.ModelSeed;
import tools.refinery.store.reasoning.translator.PartialRelationTranslator;
import tools.refinery.store.reasoning.translator.multiobject.MultiObjectTranslator;
import tools.refinery.store.representation.Symbol;
import tools.refinery.store.statecoding.StateCoderAdapter;
import tools.refinery.store.transition.system.TransitionSystemAdapter;
import tools.refinery.store.transition.system.statespace.TransitionRule;
import tools.refinery.store.transition.system.strategy.TransitionSystemStoreManager;
import tools.refinery.store.tuple.Tuple;
import tools.refinery.visualization.ModelVisualizerAdapter;
import tools.refinery.visualization.internal.FileFormat;

import java.util.List;
import java.util.function.Consumer;

import static tools.refinery.logic.literal.Literals.not;
import static tools.refinery.store.dse.transition.actions.ActionLiterals.put;
import static tools.refinery.store.reasoning.ReasoningAdapter.EQUALS_SYMBOL;
import static tools.refinery.store.reasoning.actions.PartialActionLiterals.add;
import static tools.refinery.store.reasoning.literal.PartialLiterals.must;

public class ComplexPartialTransitionSystemTest {
	PartialRelation person = new PartialRelation("Person", 1);
	PartialRelation animal = new PartialRelation("Animal", 1);
	PartialRelation friend = new PartialRelation("friend", 2);
	PartialRelation pet = new PartialRelation("pet", 2);
	PartialRelation leader = new PartialRelation("leader", 1);

	Symbol<TruthValue> personStorage = Symbol.of("Person", 1, TruthValue.class, TruthValue.FALSE);
	Symbol<TruthValue> animalStorage = Symbol.of("Animal", 1, TruthValue.class, TruthValue.FALSE);
	Symbol<TruthValue> friendStorage = Symbol.of("friend", 2, TruthValue.class, TruthValue.FALSE);
	Symbol<TruthValue> petStorage = Symbol.of("pet", 2, TruthValue.class, TruthValue.FALSE);
	Symbol<TruthValue> leaderStorage = Symbol.of("leader", 1, TruthValue.class, TruthValue.FALSE);

	Symbol<TruthValue> friendlySymbol = Symbol.of("friendly", 1, TruthValue.class, TruthValue.UNKNOWN);
	PartialRelation friendlyRelation = new PartialRelation("friendly", 1);
	RelationalQuery friendly = Query.of("friendly", (builder, p) -> builder
			.clause(p2 -> List.of(
					person.call(p),
					person.call(p2),
					friend.call(p, p2)
			))
	);

	Symbol<TruthValue> hasPetSymbol = Symbol.of("hasPet", 1, TruthValue.class, TruthValue.UNKNOWN);
	PartialRelation hasPetRelation = new PartialRelation("hasPet", 1);
	RelationalQuery hasPet = Query.of("hasPet", (builder, p) -> builder
			.clause(p2 -> List.of(
					person.call(p),
					animal.call(p2),
					pet.call(p, p2)
			))
	);

	Symbol<TruthValue> happySymbol = Symbol.of("happy", 1, TruthValue.class, TruthValue.UNKNOWN);
	PartialRelation happyRelation = new PartialRelation("happy", 1);
	RelationalQuery happy = Query.of("happy", (builder, p) -> builder
			.clause(friendly.call(p))
			.clause(hasPet.call(p))
	);

	RelationalQuery anyLeader = Query.of("anyLeader", (builder) -> builder
			.clause(p -> List.of(
					person.call(p),
					leader.call(p)
			))
	);

	PartialRelation becomesLeaderPrecondition = new PartialRelation("becomesLeader_precondition", 1);
	TransitionRule becomesLeader = new TransitionRule(
			becomesLeaderPrecondition,
			Rule.of("becomesLeader", (builder, p) -> builder
					.clause(
							person.call(p),
							happy.call(p),
							not(anyLeader.call())
					)
					.action(
							put(leaderStorage, TruthValue.TRUE, p)
					)
			)
	);

	PartialRelation estrangementPrecondition = new PartialRelation("estrangement_precondition", 2);
	TransitionRule estrange = new TransitionRule(
			estrangementPrecondition,
			Rule.of("estrange", (builder, p1, p2) -> builder
					.clause(
							person.call(p1),
							person.call(p2),
							friend.call(p1, p2)
					)
					.action(
							put(friendStorage, TruthValue.FALSE, p1, p2),
							put(friendStorage, TruthValue.FALSE, p2, p1)
					)
			)
	);

	PartialRelation lonelyMakesFriendPrecondition = new PartialRelation("lonelyMakesFriend_precondition", 2);
	TransitionRule lonelyMakesFriend = new TransitionRule(
			lonelyMakesFriendPrecondition,
			Rule.of("lonelyMakesFriend", (builder, p1, p2) -> builder
					.clause(
							person.call(p1),
							person.call(p2),
							not(EQUALS_SYMBOL.call(p1, p2)),
							not(friendly.call(p1))
					)
					.action(
							put(friendStorage, TruthValue.TRUE, p1, p2),
							put(friendStorage, TruthValue.TRUE, p2, p1)
					)
			)
	);

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
			.with(DesignSpaceExplorationAdapter.builder())
			.with(ReasoningAdapter.builder())
			.with(TransitionSystemAdapter.builder()
					.transition(becomesLeader)
					.transition(estrange)
					.transition(lonelyMakesFriend)
					.accept(_ -> () -> false) // full state space exploration
			)
			.with(ModelVisualizerAdapter.builder()
					.withOutputPath("test_output")
					.withFormat(FileFormat.SVG)
					.saveStates()
					.saveDesignSpace()
			)
			.with(new MultiObjectTranslator())
			.with(PartialRelationTranslator.of(person).symbol(personStorage))
			.with(PartialRelationTranslator.of(animal).symbol(animalStorage))
			.with(PartialRelationTranslator.of(friend).symbol(friendStorage))
			.with(PartialRelationTranslator.of(pet).symbol(petStorage))
			.with(PartialRelationTranslator.of(leader).symbol(leaderStorage))
			.with(PartialRelationTranslator.of(friendlyRelation).symbol(friendlySymbol).query(friendly))
			.with(PartialRelationTranslator.of(hasPetRelation).symbol(hasPetSymbol).query(hasPet))
			.with(PartialRelationTranslator.of(happyRelation).symbol(happySymbol).query(happy))
			.build();

	ModelSeed.Builder seed = ModelSeed.builder(3)
			.seed(MultiObjectTranslator.COUNT_SYMBOL, builder -> builder
					.reducedValue(CardinalityIntervals.ONE)
			)
			.seed(person, builder -> builder
					.put(Tuple.of(0), TruthValue.TRUE)
					.put(Tuple.of(1), TruthValue.TRUE)
			)
			.seed(animal, builder -> builder
					.put(Tuple.of(2), TruthValue.TRUE)
			)
			.seed(friend, builder -> builder
					.put(Tuple.of(0, 1), TruthValue.UNKNOWN)
					.put(Tuple.of(1, 0), TruthValue.UNKNOWN)
			)
			.seed(leader, builder -> builder
					.put(Tuple.of(0), TruthValue.UNKNOWN)
					.put(Tuple.of(1), TruthValue.UNKNOWN)
			)
			.seed(pet, builder -> builder
					.put(Tuple.of(0, 2), TruthValue.UNKNOWN)
			)
			.seed(friendlyRelation, builder -> builder
					.reducedValue(TruthValue.UNKNOWN))
			.seed(hasPetRelation, builder -> builder
					.reducedValue(TruthValue.UNKNOWN))
			.seed(happyRelation, builder -> builder
					.reducedValue(TruthValue.UNKNOWN))
			.seed(becomesLeaderPrecondition, builder -> builder
					.reducedValue(TruthValue.UNKNOWN))
			.seed(estrangementPrecondition, builder -> builder
					.reducedValue(TruthValue.UNKNOWN))
			.seed(lonelyMakesFriendPrecondition, builder -> builder
					.reducedValue(TruthValue.UNKNOWN));

	@Test
	void testTransitionSystem() {
		var seed = this.seed.build();
		performTest(seed, (manager) -> {
			var solution = manager.getSolution();
			Assertions.assertNull(solution);
		});
	}

	private void performTest(ModelSeed seed, Consumer<TransitionSystemStoreManager> check) {
		try (var model = store.getAdapter(ReasoningStoreAdapter.class).createInitialModel(seed)) {
			var queryEngine = model.getAdapter(ModelQueryAdapter.class);
			var initialVersion = model.commit();
			queryEngine.flushChanges();

			var manager = new TransitionSystemStoreManager(store);
			manager.startExploration(initialVersion);
			model.getAdapter(ModelVisualizerAdapter.class).visualize(store.getAdapter(DesignSpaceExplorationStoreAdapter.class).getStateSpaceStore(), true);
			check.accept(manager);
		}
	}

}
