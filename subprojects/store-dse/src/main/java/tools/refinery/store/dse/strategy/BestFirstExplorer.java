/*
 * SPDX-FileCopyrightText: 2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.dse.strategy;

import tools.refinery.store.dse.transition.VersionWithObjectiveValue;
import tools.refinery.store.model.Model;

import java.util.Random;

public class BestFirstExplorer extends BestFirstWorker {
	final long id;
	Random random;

	private VersionWithObjectiveValue lastBest;

	public BestFirstExplorer(BestFirstStoreManager storeManager, Model model, long id) {
		super(storeManager, model);
		this.id = id;
		// The use of a non-cryptographic random generator is safe here, because we only use it to direct the state
		// space exploration.
		@SuppressWarnings("squid:S2245")
		var randomGenerator = new Random(id);
		this.random = randomGenerator;
	}

	private boolean shouldRun() {
		model.checkCancelled();
		return !hasEnoughSolution();
	}

	public void startExploration() {
		lastBest = submit().newVersion();
		if (isVisualizationEnabled) {
			modelVisualizerAdapter.visualize(lastBest.version());
		}
	}

	public void manualStep(int numberOfSteps) {
		if (lastBest == null) {
			startExploration();
		}
		for (int i = 0; i < numberOfSteps; i++) {
			var visitResult = this.selectAndVisitUnvisited();
			if (isVisualizationEnabled) {
				modelVisualizerAdapter.visualize(visitResult.submitResult().newVersion().version());
			}
		}
	}

	public void explore() {
		startExploration();
		while (shouldRun()) {
			if (lastBest == null) {
				if (random.nextInt(10) == 0) {
					lastBest = restoreToRandom(random);
				} else {
					lastBest = restoreToBest();
				}
				if (lastBest == null) {
					return;
				}
			}
			boolean tryActivation = true;
			while (tryActivation && shouldRun()) {
				var randomVisitResult = this.visitRandomUnvisited(random);
				tryActivation = randomVisitResult.shouldRetry();
				var newSubmit = randomVisitResult.submitResult();
				if (newSubmit != null) {
					if (!newSubmit.include()) {
						restoreToLast();
					} else {
						var newVisit = newSubmit.newVersion();
						int compareResult = compare(lastBest, newVisit);
						if (compareResult >= 0)  {
							lastBest = newVisit;
						} else {
							lastBest = null;
						}
						break;
					}
				} else {
					lastBest = null;
					break;
				}
			}
		}
	}
}
