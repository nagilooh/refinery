/*
 * SPDX-FileCopyrightText: 2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.reasoning.translator;

import org.jetbrains.annotations.Nullable;
import tools.refinery.logic.dnf.AnyQuery;
import tools.refinery.store.model.ModelStoreBuilder;
import tools.refinery.store.model.ModelStoreConfiguration;
import tools.refinery.store.reasoning.representation.AnyPartialSymbol;
import tools.refinery.store.representation.AnySymbol;

public sealed interface AnyPartialSymbolTranslator extends ModelStoreConfiguration permits PartialSymbolTranslator {
	AnyPartialSymbol getPartialSymbol();

	@Nullable AnySymbol getStorageSymbol();

	@Nullable AnyQuery getQuery();

	void configure(ModelStoreBuilder storeBuilder);
}
