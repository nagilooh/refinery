package tools.refinery.store.dse.strategy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import tools.refinery.language.semantics.ProblemTrace;
import tools.refinery.store.dse.transition.Transformation;
import tools.refinery.store.dse.transition.VersionWithObjectiveValue;
import tools.refinery.store.dse.transition.statespace.ActivationStore;

import javax.swing.*;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class ManualActivationStoreWorker {
	final ActivationStore store;
	final List<Transformation> transformations;
	final ProblemTrace problemTrace;

	private static final Logger LOGGER = LoggerFactory.getLogger(ManualActivationStoreWorker.class);

	public ManualActivationStoreWorker(ActivationStore store, List<Transformation> transformations, ProblemTrace problemTrace) {
		this.store = store;
		this.transformations = transformations;
		this.problemTrace = problemTrace;
	}

	public int[] calculateEmptyActivationSize() {
		int[] result = new int[transformations.size()];
		for (int i = 0; i < result.length; i++) {
			result[i] = transformations.get(i).getAllActivationsAsResultSet().size();
		}
		return result;
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
				String namesString = allActivations.getKey(i).toString();
				var activation = allActivations.getKey(i);
				if (problemTrace != null) {
					var names = new String[activation.getSize()];
					for (int j = 0; j < names.length; j++) {
						var node = problemTrace.getIdNode(activation.get(j));
						names[j] = node == null ? String.valueOf(activation.get(j)) : node.getName();
					}
					namesString = "[" + String.join(", ", names) + "]";
				}
				String opt = String.format("%d: %s - [%d] %s", t, ruleName, i, namesString);
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
}
