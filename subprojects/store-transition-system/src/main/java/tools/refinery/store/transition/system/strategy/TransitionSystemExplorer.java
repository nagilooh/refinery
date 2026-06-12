package tools.refinery.store.transition.system.strategy;

import tools.refinery.store.dse.propagation.PropagationAdapter;
import tools.refinery.store.dse.transition.DesignSpaceExplorationAdapter;
import tools.refinery.store.map.Version;
import tools.refinery.store.model.Model;
import tools.refinery.store.query.ModelQueryAdapter;
import tools.refinery.store.statecoding.StateCoderAdapter;
import tools.refinery.store.transition.system.TransitionSystemAdapter;
import tools.refinery.store.transition.system.statespace.FiredTransition;
import tools.refinery.store.transition.system.statespace.State;
import tools.refinery.store.transition.system.statespace.Trace;
import tools.refinery.store.transition.system.statespace.internal.TransitionSystemActivationStoreWorker;
import tools.refinery.store.transition.system.strategy.concretizer.internal.DistinctVersionsTraceConcretizer;
import tools.refinery.visualization.statespace.VisualizationStore;

import java.util.Random;

public class TransitionSystemExplorer {

	private final Model model;
	private final long randomSeed;
	private final Random random;
	private final TransitionSystemStoreManager storeManager;
	private final TransitionSystemAdapter transitionSystemAdapter;
	private final DesignSpaceExplorationAdapter explorationAdapter;
	private final StateCoderAdapter stateCoderAdapter;
	private final ModelQueryAdapter queryAdapter;
	private final PropagationAdapter propagationAdapter;
	private final TransitionSystemActivationStoreWorker activationStoreWorker;
	private final VisualizationStore visualizationStore;
	private final boolean isVisualizationEnabled;

	protected State last = null;

	public TransitionSystemExplorer(TransitionSystemStoreManager storeManager, Model model, long randomSeed) {
		this.model = model;
		this.randomSeed = randomSeed;
		random = new Random(randomSeed);
		this.storeManager = storeManager;
		transitionSystemAdapter = model.getAdapter(TransitionSystemAdapter.class);
		explorationAdapter = model.getAdapter(DesignSpaceExplorationAdapter.class);
		stateCoderAdapter = model.getAdapter(StateCoderAdapter.class);
		queryAdapter = model.getAdapter(ModelQueryAdapter.class);
		propagationAdapter = model.tryGetAdapter(PropagationAdapter.class).orElse(null);
		activationStoreWorker = new TransitionSystemActivationStoreWorker(storeManager.getActivationStore(),
				transitionSystemAdapter.getTransitions());
		visualizationStore = storeManager.getVisualizationStore();
		isVisualizationEnabled = visualizationStore != null;
	}

	public void explore() {
		var last = submit(null).newState();
		while (shouldRun()) {
			if (last == null) {
				if (random.nextInt(10) == 0) {
					last = restoreToRandom(random);
				} else {
					last = restoreToBest();
				}
				if (last == null) {
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
						var newVisit = newSubmit.newState();
						int compareResult = compare(last, newVisit);
						if (compareResult >= 0)  {
							last = newVisit;
						} else {
							last = null;
						}
						break;
					}
				} else {
					last = null;
					break;
				}
			}
		}
	}

	public SubmitResult submit(FiredTransition transition) {
		if (queryAdapter.hasPendingChanges()) {
			throw new AssertionError("Pending changes detected before model submission");
		}
		if (explorationAdapter.checkExclude()) {
			return new SubmitResult(false, false, null);
		}

		var code = stateCoderAdapter.calculateStateCode();
		boolean isNew = storeManager.getEquivalenceClassStore().submit(code);
		if (isNew) {
			return submitNew(transition);
		}

		return new SubmitResult(false, false, null);
	}

	private SubmitResult submitNew(FiredTransition transition) {
		Version version = model.commit();
		int depth = last == null ? 0 : last.depth() + 1;
		last = new State(transition, version, last, depth);
		var accepted = transitionSystemAdapter.checkAccept();

		storeManager.getObjectiveStore().submit(last);
		storeManager.getActivationStore().markNewAsVisited(version, activationStoreWorker.calculateEmptyActivationSize());
		if (accepted) {
			var trace = Trace.of(last);
			var concretizedTrace = concretize(trace);
			accepted = concretizedTrace != null;

			if (accepted) {
				storeManager.setSolution(concretizedTrace);
			}
		}

		if (isVisualizationEnabled) {
			visualizationStore.addState(version, "", stateCoderAdapter.calculateModelCode());
			if (accepted) {
				visualizationStore.addSolution(version);
			}
		}

		return new SubmitResult(true, accepted, last);
	}

	public record RandomVisitResult(SubmitResult submitResult, boolean shouldRetry) {
	}

	public RandomVisitResult visitRandomUnvisited(Random random) {
		checkSynchronized();
		if (model.hasUncommittedChanges()) {
			throw new IllegalStateException("The model has uncommitted changes!");
		}

		var visitResult = activationStoreWorker.selectRandomActivation(last.version(), random);
		if (!visitResult.successfulVisit()) {
			return new RandomVisitResult(null, visitResult.mayHaveMore());
		}

		if (!propagate()) {
			return new RandomVisitResult(null, visitResult.mayHaveMore());
		}
		queryAdapter.flushChanges();

		var transition = activationStoreWorker.fireActivation(visitResult.transformation(), visitResult.activation());
		if (transition == null || !propagate()) {
			restoreToLast();
			return new RandomVisitResult(null, visitResult.mayHaveMore());
		}
		queryAdapter.flushChanges();

		State oldState = last;
		var submitResult = submit(transition);

		if (isVisualizationEnabled) {
			var label = visitResult.transformationName() + " " + visitResult.activationTuple();
			if (submitResult.newState() != null) {
				var newVersion = submitResult.newState();
				visualizationStore.addTransition(oldState.version(), newVersion.version(), label);
			} else {
				visualizationStore.addTransition(oldState.version(), stateCoderAdapter.calculateModelCode(), label);
			}
		}
		return new RandomVisitResult(submitResult, visitResult.mayHaveMore());
	}

	private Trace concretize(Trace trace) {
		var concretizer = new DistinctVersionsTraceConcretizer(trace, model, randomSeed);
		var currentState = model.getState();
		var result = concretizer.concretize();
		model.restore(currentState);
		return result;
	}

	private boolean propagate() {
		if (propagationAdapter != null) {
			var propagationResult = propagationAdapter.propagate();
			return !propagationResult.isRejected();
		}
		return true;
	}

	public int compare(State s1, State s2) {
		return storeManager.getObjectiveStore().getComparator().compare(s1, s2);
	}

	public void restoreToLast() {
		if (model.hasUncommittedChanges()) {
			model.restore(last.version());
		}
	}

	public State restoreToBest() {
		var bestState = storeManager.getObjectiveStore().getBest();
		last = bestState;
		if (bestState != null) {
			this.model.restore(bestState.version());
		}
		return last;
	}

	public State restoreToRandom(Random random) {
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

	private boolean shouldRun() {
		model.checkCancelled();
		return !hasEnoughSolution();
	}

	public boolean hasEnoughSolution() {
		return storeManager.solution != null;
	}

	private void checkSynchronized() {
		if (last != null && !last.version().equals(model.getState())) {
			throw new AssertionError("Worker is not synchronized with model state");
		}
	}
}
