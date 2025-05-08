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
	final FunctionalQuery<Double> lowQuery;
	final FunctionalQuery<Double> currentQuery;
	VersionWithObjectiveValue bestSolution = null;

	public OptimalExplorer(BestFirstStoreManager storeManager, Model model, long id, FunctionalQuery<Double> upQuery,
						   FunctionalQuery<Double> lowQuery, FunctionalQuery<Double> currentQuery) {
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
		this.lowQuery = lowQuery;
		this.currentQuery = currentQuery;
	}

	private boolean shouldRun() {
		model.checkCancelled();
		return true;
	}

	@Override
	protected SubmitResult submitNew() {
		Version version = model.commit();
		ObjectiveValue objectiveValue = ObjectiveValue.of(-1.0 * queryAdapter.getResultSet(upQuery).get(Tuple.of()));
		var versionWithObjectiveValue = new VersionWithObjectiveValue(version, objectiveValue);
		last = versionWithObjectiveValue;
		var accepted = explorationAdapter.checkAccept();

		if (bestSolution == null || storeManager.getObjectiveStore().getComparator().compare(bestSolution, last) > 0) {
			storeManager.getObjectiveStore().submit(last);
		}
		storeManager.getActivationStore().markNewAsVisited(last, activationStoreWorker.calculateEmptyActivationSize());
		if (accepted) {
			versionWithObjectiveValue = concretizeIfNeeded(versionWithObjectiveValue);
			accepted = versionWithObjectiveValue != null;
		}

		if (accepted) {
			storeManager.solutionStore.submit(versionWithObjectiveValue);
			if (bestSolution == null || storeManager.getObjectiveStore().getComparator().compare(bestSolution, versionWithObjectiveValue) > 0) {
				bestSolution = versionWithObjectiveValue;
			}
			storeManager.getObjectiveStore().removeWorse(last);
		}

		if (isVisualizationEnabled) {
			if (confidenceAggInterpretation != null) {
				visualizationStore.addState(version, confidenceAggInterpretation.get(Tuple.of()).toString() + "\n" +
						queryAdapter.getResultSet(lowQuery).get(Tuple.of()) + "\n" +
						queryAdapter.getResultSet(upQuery).get(Tuple.of()) + "\n" +
						queryAdapter.getResultSet(currentQuery).get(Tuple.of()));
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

	@Override
	public RandomVisitResult visitRandomUnvisited(Random random) {
		checkSynchronized();
		if (model.hasUncommittedChanges()) {
			throw new IllegalStateException("The model has uncommitted changes!");
		}

		var visitResult = activationStoreWorker.fireRandomActivation(this.last, random);

		if (!visitResult.successfulVisit()) {
			return new RandomVisitResult(null, visitResult.mayHaveMore());
		}

		if (propagationAdapter != null) {
			var propagationResult = propagationAdapter.propagate();
			if (propagationResult.isRejected()) {
				return new RandomVisitResult(null, visitResult.mayHaveMore());
			}
		}
		queryAdapter.flushChanges();

		Version oldVersion = null;
		if (isVisualizationEnabled) {
			oldVersion = last.version();
		}
		var submitResult = submit();
		if (isVisualizationEnabled && submitResult.newVersion() != null) {
			var newVersion = submitResult.newVersion().version();
			visualizationStore.addTransition(oldVersion, newVersion,
					"fire: " + visitResult.transformation().getDefinition().getName() + "\n" + visitResult.activation() + "\n" +
							"approx: [" + queryAdapter.getResultSet(lowQuery).get(Tuple.of()) + ", " + queryAdapter.getResultSet(upQuery).get(Tuple.of()) + "]");
		}
		return new RandomVisitResult(submitResult, visitResult.mayHaveMore());
	}

	public void explore() {
		var lastBest = submit().newVersion();
		while (shouldRun()) {
			if (lastBest == null) {
				lastBest = restoreToBest();
				if (lastBest == null) {
					visualizationStore.addBestSolution(bestSolution.version());
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
