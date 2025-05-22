/*
 * SPDX-FileCopyrightText: 2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.dse.transition.statespace.internal;

import org.junit.jupiter.api.Test;
import tools.refinery.logic.dnf.FunctionalQuery;
import tools.refinery.logic.dnf.Query;
import tools.refinery.logic.term.Variable;
import tools.refinery.store.dse.modification.ModificationAdapter;
import tools.refinery.store.dse.transition.DesignSpaceExplorationAdapter;
import tools.refinery.store.dse.transition.ObjectiveValue;
import tools.refinery.store.dse.transition.VersionWithObjectiveValue;
import tools.refinery.store.dse.transition.objectives.Objectives;
import tools.refinery.store.map.Version;
import tools.refinery.store.model.ModelStore;
import tools.refinery.store.query.ModelQueryAdapter;
import tools.refinery.store.query.interpreter.QueryInterpreterAdapter;
import tools.refinery.store.query.view.AnySymbolView;
import tools.refinery.store.query.view.KeyOnlyView;
import tools.refinery.store.representation.Symbol;
import tools.refinery.store.statecoding.StateCoderAdapter;
import tools.refinery.store.tuple.Tuple;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ComparatorTest {
	Symbol<Boolean> person = new Symbol<>("Person", 1, Boolean.class, false);
	Symbol<Boolean> friend = new Symbol<>("friend", 2, Boolean.class, false);

	AnySymbolView personView = new KeyOnlyView<>(person);
	AnySymbolView friendView = new KeyOnlyView<>(friend);

	FunctionalQuery<Integer> numberOfFriends = Query.of(Integer.class, (builder, output) -> builder
			.clause(
					output.assign(friendView.count(Variable.of(), Variable.of()))
			));

	FunctionalQuery<Integer> numberOfPeople = Query.of(Integer.class, (builder, output) -> builder
			.clause(
					output.assign(personView.count(Variable.of()))
			));

	@Test
	void compositeComparatorTest() {
		var comparator = new CompositeComparator();
		var store = ModelStore.builder()
				.symbols(person, friend)
				.with(QueryInterpreterAdapter.builder())
				.with(StateCoderAdapter.builder())
				.with(ModificationAdapter.builder())
				.with(DesignSpaceExplorationAdapter.builder()
						.objectives(Objectives.value(numberOfFriends), Objectives.value(numberOfPeople)))
				.build();
		try (var model = store.createEmptyModel()) {
			var dse = model.getAdapter(DesignSpaceExplorationAdapter.class);
			var query = model.getAdapter(ModelQueryAdapter.class);
			var personInterpretation = model.getInterpretation(person);
			var friendInterpretation = model.getInterpretation(friend);

			Version state1 = model.commit();
			var v1 = new VersionWithObjectiveValue(state1, dse.getObjectiveValue());
			Version state2 = model.commit();
			var v2 = new VersionWithObjectiveValue(state2, dse.getObjectiveValue());

			assertEquals(0, comparator.compare(v1, v2));
			assertEquals(0, comparator.compare(v2, v1));

			personInterpretation.put(Tuple.of(0), true);
			personInterpretation.put(Tuple.of(1), true);

			query.flushChanges();
			Version state3 = model.commit();
			var v3 = new VersionWithObjectiveValue(state3, dse.getObjectiveValue());

			assertEquals(-1, comparator.compare(v1, v3));
			assertEquals(1, comparator.compare(v3, v1));

			friendInterpretation.put(Tuple.of(0, 1), true);
			friendInterpretation.put(Tuple.of(1, 0), true);

			query.flushChanges();
			Version state4 = model.commit();
			var v4 = new VersionWithObjectiveValue(state4, dse.getObjectiveValue());

			assertEquals(-1, comparator.compare(v3, v4));
			assertEquals(1, comparator.compare(v4, v3));

			friendInterpretation.put(Tuple.of(1, 1), true);

			query.flushChanges();
			Version state5 = model.commit();
			var v5 = new VersionWithObjectiveValue(state5, dse.getObjectiveValue());

			assertEquals(-1, comparator.compare(v4, v5));
			assertEquals(1, comparator.compare(v5, v4));

			var v6 = new VersionWithObjectiveValue(state5, ObjectiveValue.of(0.0, 5.0));
			var v7 = new VersionWithObjectiveValue(state5, ObjectiveValue.of(5.0, 0.0));
			var v8 = new VersionWithObjectiveValue(state5, ObjectiveValue.of(4.0, 1.0));
			var v9 = new VersionWithObjectiveValue(state5, ObjectiveValue.of(0.0, 10.0));
			var v10 = new VersionWithObjectiveValue(state5, ObjectiveValue.of(4.0, 6.0));
			var v11 = new VersionWithObjectiveValue(state5, ObjectiveValue.of(1.0, 7.0));

			assertEquals(0, comparator.compare(v6, v7));
			assertEquals(0, comparator.compare(v7, v6));
			assertEquals(0, comparator.compare(v6, v8));
			assertEquals(-1, comparator.compare(v6, v9));
			assertEquals(-1, comparator.compare(v6, v10));
			assertEquals(0, comparator.compare(v9, v10));
			assertEquals(1, comparator.compare(v10, v11));
		}
	}
}
