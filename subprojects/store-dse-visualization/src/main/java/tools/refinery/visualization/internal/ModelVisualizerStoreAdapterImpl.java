/*
 * SPDX-FileCopyrightText: 2021-2026 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.visualization.internal;

import tools.refinery.store.adapter.ModelAdapter;
import tools.refinery.store.dse.transition.statespace.StateSpaceStore;
import tools.refinery.store.model.Model;
import tools.refinery.store.model.ModelStore;
import tools.refinery.visualization.ModelVisualizerStoreAdapter;

import java.util.Set;
import java.util.function.Function;

public class ModelVisualizerStoreAdapterImpl implements ModelVisualizerStoreAdapter {
	private final ModelStore store;
	private final String dotBinaryPath;
	private final String outputPath;
	private final Function<Integer, String> nodeNameProvider;
	private final boolean renderDesignSpace;
	private final boolean renderStates;
	private final Set<FileFormat> formats;
	private final StateSpaceStore stateSpaceStore;
	private final boolean hasDse;
	private final boolean hasTs;

	public ModelVisualizerStoreAdapterImpl(ModelStore store, StateSpaceStore stateSpaceStore, String dotBinaryPath, String outputPath,
										   Set<FileFormat> formats, Function<Integer, String> nodeNameProvider,
										   boolean renderDesignSpace, boolean renderStates, boolean hasDse, boolean hasTs) {
		this.store = store;
		this.stateSpaceStore = stateSpaceStore;
		this.dotBinaryPath = dotBinaryPath;
		this.outputPath = outputPath;
		this.formats = formats;
		this.nodeNameProvider = nodeNameProvider;
		this.renderDesignSpace = renderDesignSpace;
		this.renderStates = renderStates;
		this.hasDse = hasDse;
		this.hasTs = hasTs;
	}

	@Override
	public ModelStore getStore() {
		return store;
	}

	@Override
	public ModelAdapter createModelAdapter(Model model) {
		return new ModelVisualizerAdapterImpl(model, this);
	}

	String getDotBinaryPath() {
		return dotBinaryPath;
	}

	@Override
	public String getOutputPath() {
		return outputPath;
	}

	StateSpaceStore getStateSpaceStore() {
		return stateSpaceStore;
	}

	@Override
	public boolean isRenderDesignSpace() {
		return renderDesignSpace;
	}

	@Override
	public boolean isRenderStates() {
		return renderStates;
	}

	@Override
	public Set<FileFormat> getFormats() {
		return formats;
	}

	@Override
	public Function<Integer, String> getNodeNameProvider() {
		return nodeNameProvider;
	}

	@Override
	public boolean hasDesignSpaceExploration() {
		return hasDse;
	}

	@Override
	public boolean hasTransitionSystem() {
		return hasTs;
	}
}
