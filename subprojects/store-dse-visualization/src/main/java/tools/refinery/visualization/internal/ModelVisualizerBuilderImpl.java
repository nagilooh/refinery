/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.visualization.internal;

import tools.refinery.store.adapter.AbstractModelAdapterBuilder;
import tools.refinery.store.model.ModelStore;
import tools.refinery.visualization.ModelVisualizerBuilder;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.function.Function;

public class ModelVisualizerBuilderImpl
		extends AbstractModelAdapterBuilder<ModelVisualizerStoreAdapterImpl>
		implements ModelVisualizerBuilder {
	private String dotBinaryPath = "dot";
	private String outputPath;
	private Function<Integer, String> nodeNameProvider;
	private boolean saveDesignSpace = false;
	private boolean saveStates = false;
	private boolean saveTransitionsToAlreadyVisitedStates = false;
	private final Set<FileFormat> formats = new LinkedHashSet<>();

	@Override
	protected ModelVisualizerStoreAdapterImpl doBuild(ModelStore store) {
		return new ModelVisualizerStoreAdapterImpl(store, dotBinaryPath, outputPath, formats, nodeNameProvider,
				saveDesignSpace, saveStates, saveTransitionsToAlreadyVisitedStates);
	}

	@Override
	public ModelVisualizerBuilder withDotBinaryPath(String dotBinaryPath) {
		checkNotConfigured();
		this.dotBinaryPath = dotBinaryPath;
		return this;
	}

	@Override
	public ModelVisualizerBuilder withOutputPath(String outputPath) {
		checkNotConfigured();
		this.outputPath = outputPath;
		return this;
	}

	@Override
	public ModelVisualizerBuilder withFormat(FileFormat format) {
		checkNotConfigured();
		this.formats.add(format);
		return this;
	}

	@Override
	public ModelVisualizerBuilder withTrace(Function<Integer, String> nodeNameProvider) {
		checkNotConfigured();
		this.nodeNameProvider = nodeNameProvider;
		return this;
	}

	@Override
	public ModelVisualizerBuilder saveDesignSpace() {
		checkNotConfigured();
		this.saveDesignSpace = true;
		return this;
	}

	@Override
	public ModelVisualizerBuilder saveStates() {
		checkNotConfigured();
		this.saveStates = true;
		return this;
	}

	@Override
	public ModelVisualizerBuilder saveTransitionsToAlreadyVisitedStates() {
		checkNotConfigured();
		this.saveTransitionsToAlreadyVisitedStates = true;
		return this;
	}
}
