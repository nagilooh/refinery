/*
 * SPDX-FileCopyrightText: 2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.dse.transition.statespace.internal;

import tools.refinery.store.dse.transition.Transformation;
import tools.refinery.store.dse.transition.VersionWithObjectiveValue;
import tools.refinery.store.dse.transition.statespace.ActivationStore;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.concurrent.atomic.AtomicInteger;
import javax.swing.*;
import javax.swing.SwingUtilities;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ActivationStoreWorker {
	final ActivationStore store;
	final List<Transformation> transformations;
	private static final Logger LOGGER = LoggerFactory.getLogger(ActivationStoreWorker.class);

	public ActivationStoreWorker(ActivationStore store, List<Transformation> transformations) {
		this.store = store;
		this.transformations = transformations;
	}

	public int[] calculateEmptyActivationSize() {
		int[] result = new int[transformations.size()];
		for (int i = 0; i < result.length; i++) {
			result[i] = transformations.get(i).getAllActivationsAsResultSet().size();
		}
		return result;
	}

	private ActivationStore.VisitResult fireActivation(ActivationStore.VisitResult result) {
		if (result.successfulVisit()) {
			int selectedTransformation = result.transformation();
			int selectedActivation = result.activation();

			Transformation transformation = transformations.get(selectedTransformation);
			var tuple = transformation.getActivation(selectedActivation);

			boolean success = transformation.fireActivation(tuple);
			if (success) {
				return result;
			} else {
				return new ActivationStore.VisitResult(
						false,
						result.mayHaveMore(),
						selectedTransformation,
						selectedActivation);
			}
		}
		return result;
	}


	public ActivationStore.VisitResult fireRandomActivation(VersionWithObjectiveValue thisVersion, Random random) {
		var visitResult = store.getRandomAndMarkAsVisited(thisVersion, random);
		return fireActivation(visitResult);
	}

	// small holder for options -> mapping
	private static final class OptionsMapping {
		final List<String> options;
		final List<int[]> mapping;
		OptionsMapping(List<String> options, List<int[]> mapping) { this.options = options; this.mapping = mapping; }
	}

	private OptionsMapping buildOptionsMapping() {
		List<String> options = new ArrayList<>();
		List<int[]> mapping = new ArrayList<>();
		for (int t = 0; t < transformations.size(); t++) {
			Transformation tr = transformations.get(t);
			var allActivations = tr.getAllActivationsAsResultSet();
			String ruleName = "<unknown>";
			try {
				ruleName = tr.getDefinition().rule().getName();
			} catch (Exception e) {
				LOGGER.debug("Couldn't get rule name for transformation {}: {}", t, e.toString());
			}
			for (int i = 0; i < allActivations.size(); i++) {
				String opt = String.format("%d: %s - [%d] %s", t, ruleName, i, allActivations.getKey(i));
				options.add(opt);
				mapping.add(new int[]{t, i});
			}
		}
		return new OptionsMapping(options, mapping);
	}

	private int showSelectionDialog(List<String> options) {
		final AtomicInteger chosenIndex = new AtomicInteger(-1);
		try {
			SwingUtilities.invokeAndWait(() -> {
				String[] arr = options.toArray(new String[0]);
				Object selection = JOptionPane.showInputDialog(
						null,
						"Select activation to fire:",
						"Select Activation",
						JOptionPane.PLAIN_MESSAGE,
						null,
						arr,
						arr[0]);
				if (selection != null) {
					for (int i = 0; i < options.size(); i++) {
						if (options.get(i).equals(selection)) {
							chosenIndex.set(i);
							break;
						}
					}
				}
			});
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			LOGGER.error("Activation selection interrupted: {}", e.toString());
			return -1;
		} catch (InvocationTargetException e) {
			LOGGER.error("Failed to show activation selection dialog: {}", e.getCause() != null ? e.getCause().toString() : e.toString());
			return -1;
		}
		return chosenIndex.get();
	}

	public ActivationStore.VisitResult selectAndFireActivation(VersionWithObjectiveValue thisVersion) {
		OptionsMapping om = buildOptionsMapping();
		if (om.options.isEmpty()) {
			LOGGER.info("No activations available to select.");
			return new ActivationStore.VisitResult(false, false, -1, -1);
		}

		int chosen = showSelectionDialog(om.options);
		if (chosen < 0) {
			// user cancelled or dialog failed
			boolean hasMore = store.hasUnmarkedActivation(thisVersion);
			return new ActivationStore.VisitResult(false, hasMore, -1, -1);
		}

		int[] sel = om.mapping.get(chosen);
		int selectedTransformation = sel[0];
		int selectedActivation = sel[1];

		ActivationStore.VisitResult visitResult = store.visitActivation(thisVersion, selectedTransformation, selectedActivation);

		return fireActivation(visitResult);
	}
}
