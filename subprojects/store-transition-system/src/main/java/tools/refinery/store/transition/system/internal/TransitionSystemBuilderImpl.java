package tools.refinery.store.transition.system.internal;

import tools.refinery.logic.term.truthvalue.TruthValue;
import tools.refinery.store.adapter.AbstractModelAdapterBuilder;
import tools.refinery.store.dse.transition.objectives.Criterion;
import tools.refinery.store.dse.transition.statespace.StateSpaceStore;
import tools.refinery.store.model.ModelStore;
import tools.refinery.store.model.ModelStoreBuilder;
import tools.refinery.store.query.ModelQueryBuilder;
import tools.refinery.store.reasoning.representation.PartialRelation;
import tools.refinery.store.reasoning.translator.predicate.PredicateTranslator;
import tools.refinery.store.transition.system.TransitionSystemBuilder;
import tools.refinery.store.transition.system.statespace.Transition;
import tools.refinery.store.transition.system.statespace.TransitionRule;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

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
			List<PartialRelation> parameterTypes = new ArrayList<>();
			for (var i = 0; i < t.precondition().arity(); ++i) {
				parameterTypes.add(null);
			}
			var translator = new PredicateTranslator(
					t.preconditionRelation(),
					t.precondition(),
					parameterTypes,
					Set.of(),
					true,
					TruthValue.UNKNOWN
			);
			storeBuilder.with(translator);
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
