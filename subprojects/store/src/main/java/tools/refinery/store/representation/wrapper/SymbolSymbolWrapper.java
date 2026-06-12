package tools.refinery.store.representation.wrapper;

import tools.refinery.store.representation.AnySymbol;

public record SymbolSymbolWrapper(AnySymbol symbol) implements SymbolWrapper {

	@Override
	public Class<?> valueType() {
		return symbol.valueType();
	}

	@Override
	public String name() {
		return symbol.name();
	}

	@Override
	public int arity() {
		return symbol.arity();
	}
}
