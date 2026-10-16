/*
 * SPDX-FileCopyrightText: 2026 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.reasoning.translator.typehierarchy;

import tools.refinery.store.model.Model;
import tools.refinery.store.reasoning.refinement.DefaultStorageRefiner;
import tools.refinery.store.reasoning.refinement.StorageRefiner;
import tools.refinery.store.representation.Symbol;
import tools.refinery.store.tuple.Tuple;

class TypeHierarchyStorageRefiner extends DefaultStorageRefiner<InferredType> {
	private final InferredType defaultType;

	public TypeHierarchyStorageRefiner(Symbol<InferredType> symbol, Model model, InferredType defaultType) {
		super(symbol, model);
		this.defaultType = defaultType;
	}

	@Override
	public boolean createNew(int newNodeId) {
		getInterpretation().put(Tuple.of(newNodeId), defaultType);
		return super.createNew(newNodeId);
	}

	public static StorageRefiner.Factory<InferredType> factory(InferredType defaultType) {
		return (symbol, model) -> new TypeHierarchyStorageRefiner(symbol, model, defaultType);
	}
}
