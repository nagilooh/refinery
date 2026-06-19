package tools.refinery.store.reasoning.actions;

import tools.refinery.logic.AbstractValue;
import tools.refinery.logic.dnf.FunctionalQuery;
import tools.refinery.logic.term.NodeVariable;
import tools.refinery.store.dse.transition.actions.BoundActionLiteral;
import tools.refinery.store.dse.transition.actions.ComputedActionLiteral;
import tools.refinery.store.model.Model;
import tools.refinery.store.query.ModelQueryAdapter;
import tools.refinery.store.query.resultset.ResultSet;
import tools.refinery.store.reasoning.ReasoningAdapter;
import tools.refinery.store.reasoning.refinement.PartialInterpretationRefiner;
import tools.refinery.store.reasoning.representation.PartialSymbol;

import java.util.List;

public abstract class ComputedPartialActionLiteral<A extends AbstractValue<A, C>, C> extends ComputedActionLiteral<A> {
	private final PartialSymbol<A, C> partialSymbol;

	public ComputedPartialActionLiteral(PartialSymbol<A, C> partialSymbol, List<NodeVariable> parameters,
	                                    FunctionalQuery<A> valueQuery, List<NodeVariable> arguments) {
		super(parameters, valueQuery, arguments);
		if (partialSymbol.arity() != parameters.size()) {
			throw new IllegalArgumentException("Expected %d parameters for partial symbol %s, got %d instead"
					.formatted(partialSymbol.arity(), partialSymbol, parameters.size()));
		}
		this.partialSymbol = partialSymbol;
	}

	public PartialSymbol<A, C> getPartialSymbol() {
		return partialSymbol;
	}

	@Override
	public BoundActionLiteral bindToModel(Model model) {
		var refiner = model.getAdapter(ReasoningAdapter.class).getRefiner(partialSymbol);
		var resultSet = model.getAdapter(ModelQueryAdapter.class).getResultSet(valueQuery);
		return bindToModel(refiner, resultSet);
	}

	protected abstract BoundActionLiteral bindToModel(PartialInterpretationRefiner<A, C> refiner, ResultSet<A> resultSet);
}
