/*
 * SPDX-FileCopyrightText: 2026 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.reasoning.representation.wrapper;

import tools.refinery.store.reasoning.representation.AnyPartialSymbol;
import tools.refinery.store.representation.wrapper.SymbolWrapper;

public record PartialSymbolSymbolWrapper(AnyPartialSymbol partialSymbol) implements SymbolWrapper {

	@Override
	public Class<?> valueType() {
		return partialSymbol.abstractDomain().concreteType();
	}

	@Override
	public String name() {
		return partialSymbol.name();
	}

	@Override
	public int arity() {
		return partialSymbol.arity();
	}
}
