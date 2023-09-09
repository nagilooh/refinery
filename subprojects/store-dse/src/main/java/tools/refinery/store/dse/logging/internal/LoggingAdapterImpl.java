/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.dse.logging.internal;

import tools.refinery.store.dse.logging.Logger;
import tools.refinery.store.dse.logging.LoggingAdapter;
import tools.refinery.store.dse.logging.LoggingStoreAdapter;
import tools.refinery.store.dse.transition.VersionWithObjectiveValue;
import tools.refinery.store.map.Version;
import tools.refinery.store.model.Model;

import java.util.LinkedHashSet;

public class LoggingAdapterImpl implements LoggingAdapter {
	private final Model model;
	private final LoggingStoreAdapterImpl storeAdapter;
	private final LinkedHashSet<Logger> loggers;

	public LoggingAdapterImpl(Model model, LoggingStoreAdapterImpl storeAdapter) {
		this.model = model;
		this.storeAdapter = storeAdapter;
		this.loggers = storeAdapter.getLoggers();
	}

	@Override
	public Model getModel() {
		return model;
	}

	@Override
	public LoggingStoreAdapter getStoreAdapter() {
		return storeAdapter;
	}

	@Override
	public void logState(Version state) {
		loggers.forEach(logger -> logger.logState(state));
	}

	@Override
	public void logState(Version state, String label) {
		loggers.forEach(logger -> logger.logState(state, label));
	}

	@Override
	public void logState(VersionWithObjectiveValue state) {
		loggers.forEach(logger -> logger.logState(state));
	}

	@Override
	public void logState(VersionWithObjectiveValue state, String label) {
		loggers.forEach(logger -> logger.logState(state, label));
	}

	@Override
	public void logSolution(Version state) {
		loggers.forEach(logger -> logger.logSolution(state));
	}

	@Override
	public void logSolution(VersionWithObjectiveValue state) {
		loggers.forEach(logger -> logger.logSolution(state));
	}

	@Override
	public void logTransition(Version from, Version to) {
		loggers.forEach(logger -> logger.logTransition(from, to));
	}

	@Override
	public void logTransition(Version from, Version to, String label) {
		loggers.forEach(logger -> logger.logTransition(from, to, label));
	}

	@Override
	public void logTransition(VersionWithObjectiveValue from, VersionWithObjectiveValue to) {
		loggers.forEach(logger -> logger.logTransition(from, to));
	}

	@Override
	public void logTransition(VersionWithObjectiveValue from, VersionWithObjectiveValue to, String label) {
		loggers.forEach(logger -> logger.logTransition(from, to, label));
	}

	@Override
	public void flush() {
		loggers.forEach(Logger::flush);
	}
}
