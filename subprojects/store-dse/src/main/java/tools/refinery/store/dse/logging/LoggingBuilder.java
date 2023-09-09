/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.dse.logging;

import tools.refinery.store.adapter.ModelAdapterBuilder;

import java.util.Collection;
import java.util.List;

public interface LoggingBuilder extends ModelAdapterBuilder {
	LoggingBuilder withLogger(Logger logger);

	default LoggingBuilder withLoggers(Logger... loggers) {
		return withLoggers(List.of(loggers));
	}

	default LoggingBuilder withLoggers(Collection<? extends Logger> loggers) {
		loggers.forEach(this::withLogger);
		return this;
	}
}
