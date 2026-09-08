/*
 * SPDX-FileCopyrightText: 2026 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.model.wrapper;

import tools.refinery.store.map.Cursor;
import tools.refinery.store.representation.wrapper.SymbolWrapper;
import tools.refinery.store.tuple.Tuple;

public interface InterpretationWrapper<T> {

	T get(Tuple key);

	Cursor<Tuple, T> getAll();

	SymbolWrapper getSymbol();
}
