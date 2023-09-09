/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.dse.logging.loggers;

import tools.refinery.store.dse.logging.Logger;
import tools.refinery.store.map.Version;
import tools.refinery.store.model.ModelStore;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.SimpleFormatter;

public class ConsoleLogger implements Logger {
	java.util.logging.Logger logger = java.util.logging.Logger.getLogger(ConsoleLogger.class.getName());

	private final Map<Version, Integer> states = new HashMap<>();
	private Integer numberOfStates = 0;

	public ConsoleLogger setLevel(Level level) {
		logger.setLevel(level);
		return this;
	}

	@Override
	public void init(ModelStore store) {
		logger.setUseParentHandlers(false);
		var consoleHandler = new ConsoleHandler();

		consoleHandler.setFormatter(new SimpleFormatter() {
			private static final String FORMAT = "[%1$tF %1$tT] [%2$-7s] %3$s %n";

			@Override
			public synchronized String format(
					LogRecord logRecord)
			{
				return String.format(FORMAT, new Date(logRecord.getMillis()), logRecord.getLevel().getLocalizedName(),
						logRecord.getMessage());
			}
		});
		logger.addHandler(consoleHandler);
	}

	@Override
	public void flush() {
		// Everything is logged immediately
	}

	@Override
	public void logState(Version state, String label) {
		if (states.containsKey(state)) {
			return;
		}
		var stateId = numberOfStates++;
		states.put(state, stateId);
		logger.fine(() -> "State found: " + stateId + (label == null ? "" : " " + label));
	}

	@Override
	public void logSolution(Version state) {
		logger.info(() -> "Solution found: " + states.get(state));
	}

	@Override
	public void logTransition(Version from, Version to, String label) {
		logger.info(() -> "Transition fired: " + states.get(from) + " -> " + states.get(to) + (label == null ? "" :
				": " + label));
	}
}
