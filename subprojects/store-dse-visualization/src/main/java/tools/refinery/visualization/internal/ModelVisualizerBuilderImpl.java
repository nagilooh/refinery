/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.visualization.internal;

import tools.refinery.store.adapter.AbstractModelAdapterBuilder;
import tools.refinery.store.dse.transition.DesignSpaceExplorationBuilder;
import tools.refinery.store.dse.transition.statespace.StateSpaceStore;
import tools.refinery.store.dse.transition.statespace.internal.StateSpaceStoreImpl;
import tools.refinery.store.model.ModelStore;
import tools.refinery.store.model.ModelStoreBuilder;
import tools.refinery.store.transition.system.TransitionSystemBuilder;
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
	private final Set<FileFormat> formats = new LinkedHashSet<>();
	private final StateSpaceStore stateSpaceStore = new StateSpaceStoreImpl();
	private boolean hasDse;
	private boolean hasTs;

	@Override
	protected ModelVisualizerStoreAdapterImpl doBuild(ModelStore store) {
		return new ModelVisualizerStoreAdapterImpl(store, stateSpaceStore, dotBinaryPath, outputPath, formats,
				nodeNameProvider, saveDesignSpace, saveStates, hasDse, hasTs);
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
	protected void doConfigure(ModelStoreBuilder storeBuilder) {
		if (outputPath == null || outputPath.isEmpty()) {
			throw new IllegalStateException("Output path must be set for ModelVisualizerAdapter");
		}
		var dseAdapterOptional = storeBuilder.tryGetAdapter(DesignSpaceExplorationBuilder.class);
		var tsAdapterOptional = storeBuilder.tryGetAdapter(TransitionSystemBuilder.class);
		hasDse = dseAdapterOptional.isPresent();
		hasTs = tsAdapterOptional.isPresent();
		if (!hasDse && !hasTs) {
			throw new IllegalStateException(
					"ModelVisualizerAdapter requires either DesignSpaceExplorationBuilder or TransitionSystemBuilder to be present");
		}
		dseAdapterOptional.ifPresent(dseBuilder -> dseBuilder.with(stateSpaceStore));
		tsAdapterOptional.ifPresent(tsBuilder -> tsBuilder.with(stateSpaceStore));
	}
}
