/*
 * SPDX-FileCopyrightText: 2026 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.model.wrapper;

import tools.refinery.store.map.Cursor;
import tools.refinery.store.model.Interpretation;
import tools.refinery.store.representation.wrapper.SymbolSymbolWrapper;
import tools.refinery.store.representation.wrapper.SymbolWrapper;
import tools.refinery.store.tuple.Tuple;

public record InterpretationInterpretationWrapper<T>(
		Interpretation<T> interpretation) implements InterpretationWrapper<T> {

	@Override
	public T get(Tuple key) {
		return interpretation.get(key);
	}

	@Override
	public Cursor<Tuple, T> getAll() {
		return interpretation.getAll();
	}

	@Override
	public SymbolWrapper getSymbol() {
		return new SymbolSymbolWrapper(interpretation.getSymbol());
	}
}
