/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.visualization;

import tools.refinery.store.adapter.ModelAdapter;
import tools.refinery.store.dse.transition.statespace.StateSpaceStore;
import tools.refinery.store.model.wrapper.InterpretationWrapper;
import tools.refinery.store.representation.wrapper.SymbolWrapper;
import tools.refinery.visualization.internal.ModelVisualizerBuilderImpl;

import java.util.List;
import java.util.Map;

public interface ModelVisualizerAdapter extends ModelAdapter {

	ModelVisualizerStoreAdapter getStoreAdapter();

	static ModelVisualizerBuilder builder() {
		return new ModelVisualizerBuilderImpl();
	}

	default void visualize(StateSpaceStore stateSpaceStore, boolean renderTransitionsToAlreadyVisitedStates) {
		visualize(stateSpaceStore, renderTransitionsToAlreadyVisitedStates, null, null, null);
	}

	default void visualize(StateSpaceStore stateSpaceStore, boolean renderTransitionsToAlreadyVisitedStates, String subPath, String name) {
		visualize(stateSpaceStore, renderTransitionsToAlreadyVisitedStates, subPath, name, null);
	}

	default void visualize(StateSpaceStore stateSpaceStore, boolean renderTransitionsToAlreadyVisitedStates, String subPath, String name,
	               Map<SymbolWrapper, InterpretationWrapper<?>> interpretations) {
		visualize(stateSpaceStore, renderTransitionsToAlreadyVisitedStates, subPath, name, interpretations, List.of());
	}

	void visualize(StateSpaceStore stateSpaceStore, boolean renderTransitionsToAlreadyVisitedStates, String subPath, String name,
	               Map<SymbolWrapper, InterpretationWrapper<?>> interpretations, List<String> hiddenRelations);
}
