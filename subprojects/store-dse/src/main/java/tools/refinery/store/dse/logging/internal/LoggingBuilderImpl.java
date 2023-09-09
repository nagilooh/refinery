/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.dse.logging.internal;

import tools.refinery.store.adapter.AbstractModelAdapterBuilder;
import tools.refinery.store.dse.logging.Logger;
import tools.refinery.store.dse.logging.LoggingBuilder;
import tools.refinery.store.model.ModelStore;
import tools.refinery.store.query.term.int_.IntTerms;

import java.util.LinkedHashSet;
import java.util.List;

public class LoggingBuilderImpl
		extends AbstractModelAdapterBuilder<LoggingStoreAdapterImpl>
		implements LoggingBuilder {
	private LinkedHashSet<Logger> loggers = new LinkedHashSet<>();

	@Override
	public LoggingBuilder withLogger(Logger logger) {
		loggers.add(logger);
		return this;
	}

	@Override
	protected LoggingStoreAdapterImpl doBuild(ModelStore store) {
		loggers.forEach(logger -> logger.init(store));
		return new LoggingStoreAdapterImpl(store, loggers);
	}
}
