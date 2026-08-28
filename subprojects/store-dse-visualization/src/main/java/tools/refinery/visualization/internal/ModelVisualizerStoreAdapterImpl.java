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

public class ModelVisualizerStoreAdapterImpl implements ModelVisualizerStoreAdapter {
	private final ModelStore store;
	private final String dotBinaryPath;
	private final String outputPath;
	private final boolean renderDesignSpace;
	private final boolean renderStates;
	private final Set<FileFormat> formats;
	private final StateSpaceStore stateSpaceStore;

	public ModelVisualizerStoreAdapterImpl(ModelStore store, StateSpaceStore stateSpaceStore, String dotBinaryPath, String outputPath,
										   Set<FileFormat> formats, boolean renderDesignSpace, boolean renderStates) {
		this.store = store;
		this.stateSpaceStore = stateSpaceStore;
		this.dotBinaryPath = dotBinaryPath;
		this.outputPath = outputPath;
		this.formats = formats;
		this.renderDesignSpace = renderDesignSpace;
		this.renderStates = renderStates;
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
}
