package tools.refinery.store.representation.wrapper;

public interface SymbolWrapper {
	Class<?> valueType();
	String name();
	int arity();
}
