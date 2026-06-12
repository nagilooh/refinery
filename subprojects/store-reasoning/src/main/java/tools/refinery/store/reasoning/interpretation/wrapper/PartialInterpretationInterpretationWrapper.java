package tools.refinery.store.reasoning.interpretation.wrapper;

import tools.refinery.logic.AbstractValue;
import tools.refinery.store.map.Cursor;
import tools.refinery.store.model.wrapper.InterpretationWrapper;
import tools.refinery.store.reasoning.interpretation.PartialInterpretation;
import tools.refinery.store.reasoning.representation.wrapper.PartialSymbolSymbolWrapper;
import tools.refinery.store.representation.wrapper.SymbolWrapper;
import tools.refinery.store.tuple.Tuple;

public record PartialInterpretationInterpretationWrapper<A extends AbstractValue<A, C>, C>(
		PartialInterpretation<A, C> partialInterpretation) implements InterpretationWrapper<A> {
	@Override
	public A get(Tuple key) {
		return partialInterpretation.get(key);
	}

	@Override
	public Cursor<Tuple, A> getAll() {
		return partialInterpretation.getAll();
	}

	@Override
	public SymbolWrapper getSymbol() {
		return new PartialSymbolSymbolWrapper(partialInterpretation.getPartialSymbol());
	}
}
