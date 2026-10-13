/*
 * SPDX-FileCopyrightText: 2023-2026 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.reasoning.refinement;

import tools.refinery.store.model.Model;
import tools.refinery.store.representation.Symbol;

public interface StorageRefiner {
	boolean split(int parentNode, int childNode);

	boolean cleanup(int nodeToDelete);

	default boolean createNew(int newNodeId) {
		return true;
	}

	@FunctionalInterface
	interface Factory<T> {
		StorageRefiner create(Symbol<T> symbol, Model model);
	}
}
