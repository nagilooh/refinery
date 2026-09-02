/*
 * SPDX-FileCopyrightText: 2026 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.representation.wrapper;

public interface SymbolWrapper {
	Class<?> valueType();
	String name();
	int arity();
}
