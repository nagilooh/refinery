/*
 * SPDX-FileCopyrightText: 2021-2026 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.visualization;

import tools.refinery.store.adapter.ModelStoreAdapter;
import tools.refinery.visualization.internal.FileFormat;

import java.util.Set;
import java.util.function.Function;

public interface ModelVisualizerStoreAdapter extends ModelStoreAdapter {

	String getOutputPath();

	boolean isRenderDesignSpace();

	boolean isRenderStates();

	Set<FileFormat> getFormats();

	Function<Integer, String> getNodeNameProvider();

	boolean hasDesignSpaceExploration();

	boolean hasTransitionSystem();
}
