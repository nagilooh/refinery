/*
 * SPDX-FileCopyrightText: 2026 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.transition.system.internal;

import tools.refinery.store.adapter.AbstractModelAdapterBuilder;
import tools.refinery.store.dse.transition.objectives.Criterion;
import tools.refinery.store.dse.transition.statespace.StateSpaceStore;
import tools.refinery.store.model.ModelStore;
import tools.refinery.store.model.ModelStoreBuilder;
import tools.refinery.store.query.ModelQueryBuilder;
import tools.refinery.store.transition.system.TransitionSystemBuilder;
import tools.refinery.store.transition.system.statespace.Transition;
import tools.refinery.store.transition.system.statespace.TransitionRule;

import java.util.LinkedHashSet;
import java.util.List;

public class TransitionSystemBuilderImpl extends AbstractModelAdapterBuilder<TransitionSystemStoreAdapterImpl> implements TransitionSystemBuilder {

	private final LinkedHashSet<Transition.Builder> transitions = new LinkedHashSet<>();
	private final LinkedHashSet<Criterion> accepts = new LinkedHashSet<>();
	protected StateSpaceStore stateSpaceStore;

	@Override
	public TransitionSystemBuilder transition(TransitionRule rule) {
		transitions.add(Transition.Builder.of(rule));
		return this;
	}

	@Override
	public TransitionSystemBuilder accept(Criterion criterion) {
		accepts.add(criterion);
		return this;
	}

	@Override
	public TransitionSystemBuilder with(StateSpaceStore stateSpaceStore) {
		this.stateSpaceStore = stateSpaceStore;
		return this;
	}

	@Override
	protected void doConfigure(ModelStoreBuilder storeBuilder) {
		var queryEngine = storeBuilder.getAdapter(ModelQueryBuilder.class);
		transitions.forEach(t -> {
			queryEngine.queries(t.mayPrecondition());
			for (var literal : t.action().getActionLiterals()) {
				queryEngine.queries(literal.getQueries());
			}
		});
		accepts.forEach(x -> x.configure(storeBuilder));
		super.doConfigure(storeBuilder);
	}

	@Override
	protected TransitionSystemStoreAdapterImpl doBuild(ModelStore store) {
		List<Transition.Builder> transitionsList = List.copyOf(transitions);
		List<Criterion> acceptsList = List.copyOf(accepts);

		return new TransitionSystemStoreAdapterImpl(store, transitionsList, acceptsList);
	}
}
