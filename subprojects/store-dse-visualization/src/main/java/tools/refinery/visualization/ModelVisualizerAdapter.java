/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.visualization;

import tools.refinery.store.adapter.ModelAdapter;
import tools.refinery.store.model.wrapper.InterpretationWrapper;
import tools.refinery.store.representation.wrapper.SymbolWrapper;
import tools.refinery.visualization.internal.ModelVisualizerBuilderImpl;
import tools.refinery.visualization.statespace.VisualizationStore;

import java.util.Map;

public interface ModelVisualizerAdapter extends ModelAdapter {

	ModelVisualizerStoreAdapter getStoreAdapter();

	static ModelVisualizerBuilder builder() {
		return new ModelVisualizerBuilderImpl();
	}

	default void visualize(VisualizationStore visualizationStore) {
		visualize(visualizationStore, null, null, null);
	}

	default void visualize(VisualizationStore visualizationStore, String subPath, String name) {
		visualize(visualizationStore, subPath, name, null);
	}

	void visualize(VisualizationStore visualizationStore, String subPath, String name,
				   Map<SymbolWrapper, InterpretationWrapper<?>> interpretations);
}
