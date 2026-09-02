/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.visualization;

import tools.refinery.store.adapter.ModelAdapterBuilder;
import tools.refinery.visualization.internal.FileFormat;

import java.util.function.Function;

public interface ModelVisualizerBuilder extends ModelAdapterBuilder {
	ModelVisualizerBuilder withDotBinaryPath(String dotBinaryPath);
	ModelVisualizerBuilder withOutputPath(String outputPath);
	ModelVisualizerBuilder withFormat(FileFormat format);
	ModelVisualizerBuilder withTrace(Function<Integer, String> nodeNameProvider);
	ModelVisualizerBuilder saveDesignSpace();
	ModelVisualizerBuilder saveStates();
}
