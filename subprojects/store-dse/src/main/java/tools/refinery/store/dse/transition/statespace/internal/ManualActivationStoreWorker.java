package tools.refinery.store.dse.transition.statespace.internal;

import tools.refinery.store.dse.transition.Transformation;
import tools.refinery.store.dse.transition.VersionWithObjectiveValue;
import tools.refinery.store.dse.transition.statespace.ActivationStore;

import javax.swing.*;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

public class ManualActivationStoreWorker extends ActivationStoreWorker {

//	private static final Logger LOGGER = LoggerFactory.getLogger(ManualActivationStoreWorker.class);
	// Set to true to force the CLI selection dialog; set to false to use the original Swing dialog.
	private static final boolean USE_CLI = true;

	public ManualActivationStoreWorker(ActivationStore store, List<Transformation> transformations) {
		super(store, transformations);
	}

	// small holder for options -> mapping
	private static final class OptionsMapping {
		final List<String> options;
		final List<int[]> mapping;

		OptionsMapping(List<String> options, List<int[]> mapping) {
			this.options = options;
			this.mapping = mapping;
		}
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
//				LOGGER.debug("Couldn't get rule name for transformation {}: {}", t, e.toString());
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
		// Toggle between original Swing dialog (default) and CLI dialog by changing USE_CLI.
		if (USE_CLI) {
			return showSelectionDialogCli(options);
		}

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
			return -1;
		} catch (InvocationTargetException e) {
			// If Swing fails, fall back to CLI selection.
			return showSelectionDialogCli(options);
		}
		return chosenIndex.get();
	}

	private int showSelectionDialogCli(List<String> options) {
		BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));
		try {
			System.out.println("Select activation to fire:");
			for (int i = 0; i < options.size(); i++) {
				System.out.printf("  %d) %s%n", i, options.get(i));
			}
			System.out.println("Enter number to select, or 'c' to cancel:");
			while (true) {
				System.out.print("> ");
				String line = reader.readLine();
				if (line == null) {
					// EOF -> treat as cancel
					return -1;
				}
				line = line.trim();
				if (line.isEmpty()) {
					continue;
				}
				if (line.equalsIgnoreCase("c") || line.equalsIgnoreCase("q")) {
					return -1;
				}
				try {
					int idx = Integer.parseInt(line);
					if (idx >= 0 && idx < options.size()) {
						return idx;
					} else {
						System.out.printf("Invalid selection: %d (valid range 0..%d)%n", idx, options.size() - 1);
					}
				} catch (NumberFormatException nfe) {
					System.out.println("Please enter a valid number, or 'c' to cancel.");
				}
			}
		} catch (IOException ioe) {
			return -1;
		}
	}

	public ActivationStore.VisitResult selectAndFireActivation(VersionWithObjectiveValue thisVersion) {
		OptionsMapping om = buildOptionsMapping();
		if (om.options.isEmpty()) {
//			LOGGER.info("No activations available to select.");
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

		ActivationStore.VisitResult visitResult = store.visitActivation(thisVersion, selectedTransformation,
				selectedActivation);

		return visitActivation(visitResult);
	}
}
