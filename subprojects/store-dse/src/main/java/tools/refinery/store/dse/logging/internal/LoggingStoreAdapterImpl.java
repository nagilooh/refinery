/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.dse.logging.internal;

import tools.refinery.store.adapter.ModelAdapter;
import tools.refinery.store.dse.logging.Logger;
import tools.refinery.store.dse.logging.LoggingStoreAdapter;
import tools.refinery.store.model.Model;
import tools.refinery.store.model.ModelStore;

import java.util.LinkedHashSet;

public class LoggingStoreAdapterImpl implements LoggingStoreAdapter {
	private final ModelStore store;

	private final LinkedHashSet<Logger> loggers;

	public LoggingStoreAdapterImpl(ModelStore store, LinkedHashSet<Logger> loggers) {
		this.store = store;
		this.loggers = loggers;
	}

	@Override
	public ModelStore getStore() {
		return store;
	}

	@Override
	public ModelAdapter createModelAdapter(Model model) {
		return new LoggingAdapterImpl(model, this);
	}

	@Override
	public LinkedHashSet<Logger> getLoggers() {
		return loggers;
	}
}
