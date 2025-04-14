/*
 * SPDX-FileCopyrightText: 2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.dse.strategy;

import tools.refinery.logic.dnf.FunctionalQuery;
import tools.refinery.store.dse.transition.ObjectiveValue;
import tools.refinery.store.dse.transition.VersionWithObjectiveValue;
import tools.refinery.store.map.Version;
import tools.refinery.store.model.Interpretation;
import tools.refinery.store.model.Model;
import tools.refinery.store.representation.Symbol;
import tools.refinery.store.tuple.Tuple;

import java.util.Random;

public class OptimalExplorer extends BestFirstWorker {
	final long id;
	Random random;
	final Interpretation<Double> confidenceAggInterpretation;
	final FunctionalQuery<Double> upQuery;

	public OptimalExplorer(BestFirstStoreManager storeManager, Model model, long id, FunctionalQuery<Double> upQuery) {
		super(storeManager, model);
		this.id = id;
		// The use of a non-cryptographic random generator is safe here, because we only use it to direct the state
		// space exploration.
		@SuppressWarnings("squid:S2245")
		var randomGenerator = new Random(id);
		this.random = randomGenerator;

		var confidenceAggOpt = model.getStore().getSymbols().stream().filter(anySymbol -> anySymbol.name().equals(
				"confidenceAgg")).findAny();
		var confidenceAgg = (Symbol<Double>) confidenceAggOpt.orElse(null);
		if (confidenceAgg == null) {
			confidenceAggInterpretation = null;
		}
		else {
			confidenceAggInterpretation = model.getInterpretation(confidenceAgg);
		}
		this.upQuery = upQuery;
	}

	private boolean shouldRun() {
		model.checkCancelled();
		return !hasEnoughSolution();
	}

	@Override
	protected SubmitResult submitNew() {
		Version version = model.commit();
		ObjectiveValue objectiveValue = ObjectiveValue.of(queryAdapter.getResultSet(upQuery).get(Tuple.of()));
		var versionWithObjectiveValue = new VersionWithObjectiveValue(version, objectiveValue);
		last = versionWithObjectiveValue;
		var accepted = explorationAdapter.checkAccept();

		storeManager.getObjectiveStore().submit(last);
		storeManager.getActivationStore().markNewAsVisited(last, activationStoreWorker.calculateEmptyActivationSize());
		if (accepted) {
			versionWithObjectiveValue = concretizeIfNeeded(versionWithObjectiveValue);
			accepted = versionWithObjectiveValue != null;
		}

		if (accepted) {
			storeManager.solutionStore.submit(versionWithObjectiveValue);
		}

		if (isVisualizationEnabled) {
			if (confidenceAggInterpretation != null) {
				visualizationStore.addState(version, confidenceAggInterpretation.get(Tuple.of()).toString());
			}
			else {
				visualizationStore.addState(version, ":)");
			}
			if (accepted) {
				visualizationStore.addSolution(version);
			}
		}

		return new SubmitResult(true, accepted, objectiveValue, last);
	}

	public void explore() {
		var lastBest = submit().newVersion();
		while (shouldRun()) {
			if (lastBest == null) {
				lastBest = restoreToBest();
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
