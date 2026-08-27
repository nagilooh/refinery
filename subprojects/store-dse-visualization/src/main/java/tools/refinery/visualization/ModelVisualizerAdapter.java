/*
 * SPDX-FileCopyrightText: 2021-2026 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.visualization;

import tools.refinery.store.adapter.ModelAdapter;
import tools.refinery.store.dse.transition.statespace.StateSpaceStore;
import tools.refinery.visualization.internal.ModelVisualizerBuilderImpl;

public interface ModelVisualizerAdapter extends ModelAdapter {

	ModelVisualizerStoreAdapter getStoreAdapter();
	static ModelVisualizerBuilder builder() {
		return new ModelVisualizerBuilderImpl();
	}
	void visualize(StateSpaceStore stateSpaceStore);
}
