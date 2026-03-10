/*
 * SPDX-FileCopyrightText: 2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.dse.strategy;

import org.jetbrains.annotations.Nullable;
import tools.refinery.logic.dnf.AnyQuery;
import tools.refinery.store.dse.propagation.PropagationAdapter;
import tools.refinery.store.dse.transition.DesignSpaceExplorationAdapter;
import tools.refinery.store.dse.transition.ObjectiveValue;
import tools.refinery.store.dse.transition.VersionWithObjectiveValue;
import tools.refinery.store.dse.transition.statespace.internal.ActivationStoreWorker;
import tools.refinery.store.map.Version;
import tools.refinery.store.model.Model;
import tools.refinery.store.query.ModelQueryAdapter;
import tools.refinery.store.query.resultset.AnyResultSet;
import tools.refinery.store.statecoding.StateCoderAdapter;
import tools.refinery.visualization.statespace.VisualizationStore;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.Instant;
import java.util.Map;
import java.util.Random;

public class BestFirstWorker {
	final BestFirstStoreManager storeManager;
	final Model model;
	final ActivationStoreWorker activationStoreWorker;
	final StateCoderAdapter stateCoderAdapter;
	final DesignSpaceExplorationAdapter explorationAdapter;
	final ModelQueryAdapter queryAdapter;
	final @Nullable PropagationAdapter propagationAdapter;
	final VisualizationStore visualizationStore;
	final boolean isVisualizationEnabled;

	public BestFirstWorker(BestFirstStoreManager storeManager, Model model) {
		this.storeManager = storeManager;
		this.model = model;

		explorationAdapter = model.getAdapter(DesignSpaceExplorationAdapter.class);
		stateCoderAdapter = model.getAdapter(StateCoderAdapter.class);
		queryAdapter = model.getAdapter(ModelQueryAdapter.class);
		propagationAdapter = model.tryGetAdapter(PropagationAdapter.class).orElse(null);
		activationStoreWorker = new ActivationStoreWorker(storeManager.getActivationStore(),
				explorationAdapter.getTransformations());
		visualizationStore = storeManager.getVisualizationStore();
		isVisualizationEnabled = visualizationStore != null;
	}

	protected VersionWithObjectiveValue last = null;

	public SubmitResult submit() {
		checkSynchronized();
		if (queryAdapter.hasPendingChanges()) {
			throw new AssertionError("Pending changes detected before model submission");
		}
		if (explorationAdapter.checkExclude()) {
			return new SubmitResult(false, false, null, null);
		}

		var code = stateCoderAdapter.calculateStateCode();
		boolean isNew = storeManager.getEquivalenceClassStore().submit(code);
		if (isNew) {
			return submitNew();
		}

		return new SubmitResult(false, false, null, null);
	}

	private SubmitResult submitNew() {
		Version version = model.commit();
		ObjectiveValue objectiveValue = explorationAdapter.getObjectiveValue();
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


		visualizationStore.addState(version, objectiveValue.toString());
		if (accepted) {
			visualizationStore.addSolution(version);
		}
		saveResultSets(version, queryAdapter.getResultSets());

		return new SubmitResult(true, accepted, objectiveValue, last);
	}

	private void saveResultSets(Version version, Map<AnyQuery, AnyResultSet> resultSets) {
		File csvOutputFile = new File("query-results-at-commit.csv");
		boolean needHeader = !csvOutputFile.exists() || csvOutputFile.length() == 0L;
		try (FileWriter fw = new FileWriter(csvOutputFile, true)) {
			if (needHeader) {
				fw.write(headerLine(resultSets));
				fw.write("\n");
			}
			fw.write(resultSetToString(version, resultSets));
			fw.write("\n");
		} catch (IOException e) {
			throw new java.io.UncheckedIOException(e);
		}
	}

	private String headerLine(Map<AnyQuery, AnyResultSet> resultSets) {
		StringBuilder sb = new StringBuilder();
		sb.append("timestamp");
		// state hash column
		sb.append(",stateHash");
		for (Map.Entry<AnyQuery, AnyResultSet> entry : resultSets.entrySet()) {
			sb.append(',');
			// use query name as column header
			sb.append(escapeCsv(entry.getKey().name()));
		}
		return sb.toString();
	}

	private String resultSetToString(Version version, Map<AnyQuery, AnyResultSet> resultSets) {
		StringBuilder sb = new StringBuilder();
		// ISO-8601 timestamp
		sb.append(Instant.now().toString());
		// append model state hash code (0 if state is null)
		sb.append(',');
		int stateHash = (version == null) ? 0 : version.hashCode();
		sb.append(stateHash);
		for (Map.Entry<AnyQuery, AnyResultSet> entry : resultSets.entrySet()) {
			sb.append(',');
			AnyResultSet rs = entry.getValue();
			long size = rs == null ? 0L : rs.size();
			sb.append(size);
		}
		return sb.toString();
	}

	private String escapeCsv(String s) {
		if (s == null) return "";
		if (s.contains(",") || s.contains("\"") || s.contains("\n")) {
			// escape by wrapping in quotes and doubling internal quotes
			return '"' + s.replace("\"", "\"\"") + '"';
		}
		return s;
	}

	private VersionWithObjectiveValue concretizeIfNeeded(VersionWithObjectiveValue originalValue) {
		if (propagationAdapter == null) {
			return originalValue;
		}
		var version = originalValue.version();
		if (propagationAdapter.concretizationRequested()) {
			var concretizationResult = propagationAdapter.concretize();
			if (concretizationResult.isRejected()) {
				model.restore(version);
				return null;
			} else if (concretizationResult.isChanged()) {
				var newValue = submitConcrete();
				model.restore(version);
				return newValue;
			}
		} else if (propagationAdapter.checkConcretization().isRejected()) {
			return null;
		}
		return originalValue;
	}

	private VersionWithObjectiveValue submitConcrete() {
		if (queryAdapter.hasPendingChanges()) {
			throw new AssertionError("Pending changes detected before model submission");
		}
		if (explorationAdapter.checkExclude()) {
			return null;
		}

		var code = stateCoderAdapter.calculateStateCode();
		if (!storeManager.getEquivalenceClassStore().submit(code)) {
			return null;
		}

		var concreteVersion = model.commit();
		var concreteObjectiveValue = explorationAdapter.getObjectiveValue();
		var versionWithObjectiveValue = new VersionWithObjectiveValue(concreteVersion, concreteObjectiveValue);
		return explorationAdapter.checkAccept() ? versionWithObjectiveValue : null;
	}

	public void restoreToLast() {
		if (explorationAdapter.getModel().hasUncommittedChanges()) {
			explorationAdapter.getModel().restore(last.version());
		}
	}

	public VersionWithObjectiveValue restoreToBest() {
		var bestVersion = storeManager.getObjectiveStore().getBest();
		last = bestVersion;
		if (bestVersion != null) {
			this.model.restore(bestVersion.version());
		}
		return last;
	}

	public VersionWithObjectiveValue restoreToRandom(Random random) {
		var objectiveStore = storeManager.getObjectiveStore();
		if (objectiveStore.getSize() == 0) {
			return null;
		}
		var randomVersion = objectiveStore.getRandom(random);
		last = randomVersion;
		if (randomVersion != null) {
			this.model.restore(randomVersion.version());
		}
		return last;
	}

	public int compare(VersionWithObjectiveValue s1, VersionWithObjectiveValue s2) {
		return storeManager.getObjectiveStore().getComparator().compare(s1, s2);
	}

	public record RandomVisitResult(SubmitResult submitResult, boolean shouldRetry) {
	}

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
					"fire: " + visitResult.transformation() + ", " + visitResult.activation());
		}
		return new RandomVisitResult(submitResult, visitResult.mayHaveMore());
	}

	public boolean hasEnoughSolution() {
		return storeManager.solutionStore.hasEnoughSolution();
	}

	private void checkSynchronized() {
		if (last != null && !last.version().equals(model.getState())) {
			throw new AssertionError("Worker is not synchronized with model state");
		}
	}
}
