package tools.refinery.store.transition.system.strategy.concretizer.internal;

import tools.refinery.logic.AbstractValue;
import tools.refinery.logic.dnf.Dnf;
import tools.refinery.logic.literal.CallLiteral;
import tools.refinery.logic.term.NodeVariable;
import tools.refinery.logic.term.Variable;
import tools.refinery.store.dse.propagation.PropagationAdapter;
import tools.refinery.store.dse.transition.DecisionRule;
import tools.refinery.store.dse.transition.DesignSpaceExplorationAdapter;
import tools.refinery.store.dse.transition.DesignSpaceExplorationStoreAdapter;
import tools.refinery.store.dse.transition.ObjectiveValue;
import tools.refinery.store.dse.transition.ObjectiveValues.ObjectiveValue1;
import tools.refinery.store.dse.transition.VersionWithObjectiveValue;
import tools.refinery.store.dse.transition.actions.Action;
import tools.refinery.store.dse.transition.actions.ActionLiteral;
import tools.refinery.store.dse.transition.actions.BoundAction;
import tools.refinery.store.dse.transition.actions.ComputedActionLiteral;
import tools.refinery.store.dse.transition.statespace.ActivationStore;
import tools.refinery.store.dse.transition.statespace.EquivalenceClassStore;
import tools.refinery.store.dse.transition.statespace.ObjectivePriorityQueue;
import tools.refinery.store.dse.transition.statespace.SolutionStore;
import tools.refinery.store.dse.transition.statespace.internal.ActivationStoreImpl;
import tools.refinery.store.dse.transition.statespace.internal.ActivationStoreWorker;
import tools.refinery.store.dse.transition.statespace.internal.FastEquivalenceClassStore;
import tools.refinery.store.dse.transition.statespace.internal.ObjectivePriorityQueueImpl;
import tools.refinery.store.dse.transition.statespace.internal.SolutionStoreImpl;
import tools.refinery.store.map.Version;
import tools.refinery.store.map.VersionedMap;
import tools.refinery.store.map.VersionedMapStore;
import tools.refinery.store.model.Model;
import tools.refinery.store.query.ModelQueryAdapter;
import tools.refinery.store.query.resultset.ResultSetListener;
import tools.refinery.store.reasoning.ReasoningAdapter;
import tools.refinery.store.reasoning.actions.ComputedMergeActionLiteral;
import tools.refinery.store.reasoning.actions.JoinActionLiteral;
import tools.refinery.store.reasoning.actions.MergeActionLiteral;
import tools.refinery.store.reasoning.actions.ModifyActionLiteral;
import tools.refinery.store.reasoning.interpretation.PartialInterpretation;
import tools.refinery.store.reasoning.literal.Concreteness;
import tools.refinery.store.reasoning.refinement.ConcreteRelationRefiner;
import tools.refinery.store.reasoning.refinement.PartialInterpretationRefiner;
import tools.refinery.store.reasoning.representation.PartialRelation;
import tools.refinery.store.reasoning.representation.PartialSymbol;
import tools.refinery.store.representation.Symbol;
import tools.refinery.store.statecoding.StateCoderAdapter;
import tools.refinery.store.statecoding.StateCoderStoreAdapter;
import tools.refinery.store.transition.system.statespace.FiredTransition;
import tools.refinery.store.transition.system.statespace.State;
import tools.refinery.store.transition.system.statespace.Trace;
import tools.refinery.store.transition.system.strategy.concretizer.TraceConcretizer;
import tools.refinery.store.tuple.Tuple;

import java.util.AbstractMap.SimpleEntry;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Set;
import java.util.Stack;
import java.util.function.Consumer;

public class DistinctVersionsTraceConcretizer extends TraceConcretizer {

	private final Random random;

	private final Model model;
	private final DesignSpaceExplorationAdapter explorationAdapter;
	private final DesignSpaceExplorationStoreAdapter explorationStoreAdapter;
	private final StateCoderAdapter stateCoderAdapter;
	private final ModelQueryAdapter queryAdapter;
	private final PropagationAdapter propagationAdapter;
	private final ReasoningAdapter reasoningAdapter;
	private final List<PartialRelationChangeManager<?, ?>> partialSymbolListeners;

	private final List<ActivationStoreWorker<Version>> activationStoreWorkers;
	private final List<ActivationStore<Version>> activationStores;
	private final EquivalenceClassStore[] equivalenceClassStores;
	private final SolutionStore[] solutionStores;

	private final ObjectivePriorityQueue<TraceVersion> objectiveStore =
			new ObjectivePriorityQueueImpl<>(Comparator
					.comparing(TraceVersion::concretizedIndex).reversed()
					.thenComparing(v -> ((ObjectiveValue1) v.stateVersion.objectiveValue()).value0()));

	private final Consumer<Version> actionWhenAllActiovationVisited = (Version version) ->
			objectiveStore.removeIf(v ->
					v.concretizedIndex == currentlyConcretized() && v.stateVersion.version().equals(version));

	private final VersionedMap<Integer, Version> currentVersions;
	private final ConcretizationOrderManager concretizationOrderManager;
	private TraceVersion last;

	public DistinctVersionsTraceConcretizer(Trace trace, Model model, long randomSeed) {
		super(trace);

		this.model = model;
		random = new Random(randomSeed);
		explorationAdapter = model.getAdapter(DesignSpaceExplorationAdapter.class);
		stateCoderAdapter = model.getAdapter(StateCoderAdapter.class);
		queryAdapter = model.getAdapter(ModelQueryAdapter.class);
		propagationAdapter = model.tryGetAdapter(PropagationAdapter.class).orElse(null);
		explorationStoreAdapter = model.getStore().getAdapter(DesignSpaceExplorationStoreAdapter.class);
		reasoningAdapter = model.getAdapter(ReasoningAdapter.class);

		var partialSymbols = reasoningAdapter.getStoreAdapter().getRefinablePartialSymbols();
		partialSymbolListeners = new ArrayList<>(partialSymbols.size());
		for (var partialSymbol : partialSymbols) {
			partialSymbolListeners.add(new PartialRelationChangeManager<>((PartialSymbol<?, ?>) partialSymbol));
		}

		if (explorationStoreAdapter.getObjectives().size() != 1) {
			throw new UnsupportedOperationException("Only single objective comparator is implemented currently!");
		}

		var store = VersionedMapStore.<Integer, Version>builder().defaultValue(null).build().createOne();
		currentVersions = store.createMap();

		activationStoreWorkers = new ArrayList<>(trace.states().size());
		activationStores = new ArrayList<>(trace.states().size());
		equivalenceClassStores = new EquivalenceClassStore[trace.states().size()];
		solutionStores = new SolutionStore[trace.states().size()];

		for (int i = 0; i < trace.states().size(); i++) {
			activationStoreWorkers.add(null);
			activationStores.add(null);
			equivalenceClassStores[i] = null;
			solutionStores[i] = null;
		}

		concretizationOrderManager = new BackwardConcretizationOrder(trace.states().size());
	}

	private TraceVersion recreateCurrentStores() {
		var activationStore = new ActivationStoreImpl<>(explorationStoreAdapter.getTransformations(),
				DecisionRule::getWeight, actionWhenAllActiovationVisited);
		activationStoreWorkers.set(currentlyConcretized(),
				new ActivationStoreWorker<>(activationStore, explorationAdapter.getTransformations()));
		activationStores.set(currentlyConcretized(), activationStore);
		equivalenceClassStores[currentlyConcretized()] = new FastEquivalenceClassStore(model.getStore().getAdapter(StateCoderStoreAdapter.class)) {
			@Override
			protected void delegate(VersionWithObjectiveValue version, int[] emptyActivations, boolean accept) {
				throw new UnsupportedOperationException("This equivalence storage is not prepared to resolve " +
						"symmetries!");
			}
		};
		solutionStores[currentlyConcretized()] = new SolutionStoreImpl(1);

		model.restore(currentVersion());
		activationStores.get(currentlyConcretized()).markNewAsVisited(currentVersion(),
				activationStoreWorkers.get(currentlyConcretized()).calculateEmptyActivationSize());

		return submit().newVersion();
	}

	private int currentlyConcretized() {
		return concretizationOrderManager.getCurrentConcretizedIndex();
	}

	private Version currentVersion() {
		return currentVersions.get(currentlyConcretized());
	}

	@Override
	public Trace concretize() {
		for (int i = 0; i < traceToConcretize.states().size(); i++) {
			var state = traceToConcretize.states().get(i);
			currentVersions.put(i, state.version());
		}
		concretizationOrderManager.reset();
		recreateCurrentStores();

		if (!explore()) {
			return null;
		}

		var states = new ArrayList<State>(traceToConcretize.states().size());
		State previousState = null;
		for (int i = 0; i < traceToConcretize.states().size(); i++) {
			var incomingTransition = i == 0 ? null : traceToConcretize.transitions().get(i - 1);
			var version = currentVersions.get(i);
			State state = new State(incomingTransition, version, previousState, i);
			states.add(state);
			previousState = state;
		}
		return Trace.of(states, traceToConcretize.transitions());
	}

	private boolean shouldRun() {
		model.checkCancelled();
		return !hasEnoughSolution();
	}

	private boolean explore() {
		var lastBest = submit().newVersion();
		while (shouldRun() || concretizationOrderManager.hasMore()) {
			if (hasEnoughSolution()) {
				concretizationOrderManager.advance();
				if (!concretizationOrderManager.hasMore()) {
					return true; // trace is concretized
				}

				lastBest = recreateCurrentStores();
			}

			if (lastBest == null) {
				if (random.nextInt(10) == 0) {
					lastBest = restoreToRandom(random);
				} else {
					lastBest = restoreToBest();
				}
				if (lastBest == null) {
					return false; // unsat
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
						if (compareResult >= 0) {
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
		throw new IllegalStateException("Exploration finished without concretizing the whole trace, this should not happen!");
	}

	private SubmitResult submit() {
		var tracePropagationResult = propagateAlongTrace(false);
		model.restore(currentVersion());
		if (!tracePropagationResult) {
			return new SubmitResult(false, false, null, null);
		}
		queryAdapter.flushChanges();

		if (queryAdapter.hasPendingChanges()) {
			throw new AssertionError("Pending changes detected before model submission");
		}
		if (explorationAdapter.checkExclude()) {
			return new SubmitResult(false, false, null, null);
		}

		var code = stateCoderAdapter.calculateStateCode();
		boolean isNew = equivalenceClassStores[currentlyConcretized()].submit(code);
		if (isNew) {
			return submitNew();
		}

		return new SubmitResult(false, false, null, null);
	}

	private SubmitResult submitNew() {
		Version version = model.commit();
		ObjectiveValue objectiveValue = explorationAdapter.getObjectiveValue();
		var versionWithObjectiveValue = new VersionWithObjectiveValue(version, objectiveValue);

		currentVersions.put(currentlyConcretized(), version);
		var accepted = explorationAdapter.checkAccept();
		if (accepted) {
			accepted = propagateAlongTrace(true);
			model.restore(currentVersion());
		}

		Version traceVersion = currentVersions.commit();
		last = new TraceVersion(currentlyConcretized(), versionWithObjectiveValue, traceVersion);
		objectiveStore.submit(last);

		activationStores.get(currentlyConcretized()).markNewAsVisited(version,
				activationStoreWorkers.get(currentlyConcretized()).calculateEmptyActivationSize());

		if (accepted) {
			versionWithObjectiveValue = concretizeIfNeeded(versionWithObjectiveValue);
			if (versionWithObjectiveValue != null) {
				solutionStores[currentlyConcretized()].submit(versionWithObjectiveValue);
			}
		}

		return new SubmitResult(true, accepted, objectiveValue, last);
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
		if (!equivalenceClassStores[currentlyConcretized()].submit(code)) {
			return null;
		}

		var concreteVersion = model.commit();
		var concreteObjectiveValue = explorationAdapter.getObjectiveValue();
		var versionWithObjectiveValue = new VersionWithObjectiveValue(concreteVersion, concreteObjectiveValue);
		return explorationAdapter.checkAccept() ? versionWithObjectiveValue : null;
	}

	private RandomVisitResult visitRandomUnvisited(Random random) {
		if (model.hasUncommittedChanges()) {
			throw new IllegalStateException("The model has uncommitted changes!");
		}

		var visitResult =
				activationStoreWorkers.get(currentlyConcretized()).fireRandomActivation(currentVersion(), random);

		if (!visitResult.successfulVisit()) {
			return new RandomVisitResult(null, visitResult.mayHaveMore());
		}

		if (propagationAdapter != null) {
			var propagationResult = propagationAdapter.propagate();
			if (propagationResult.isRejected()) {
				return new RandomVisitResult(null, visitResult.mayHaveMore());
			}
		}

		currentVersions.put(currentlyConcretized(), model.commit());

		var submitResult = submit();
		return new RandomVisitResult(submitResult, visitResult.mayHaveMore());
	}

	private void fixCandidateValuesAssumedOrUsedByTransition(FiredTransition transition) {
		var symbolsToFix = new HashSet<Entry<PartialSymbol<?, ?>, Tuple>>();

		var activation = transition.activation();

		var preconditionAction = transition.transition().getPreconditionAction();
		collectUsedPartialSymbols(preconditionAction, activation, symbolsToFix);

		var boundAction = transition.transition().getAction();
		collectUsedPartialSymbols(boundAction, activation, symbolsToFix);

		var newItems = new Stack<Entry<PartialSymbol<?, ?>, Tuple>>();
		for (var symbolToFix : symbolsToFix) {
			newItems.push(symbolToFix);
		}

		while (!newItems.isEmpty()) {
			var current = newItems.pop();
			var refiner = reasoningAdapter.getRefiner(current.getKey());
			if (refiner instanceof ConcreteRelationRefiner concreteRelationRefiner) {
				var childSymbols = concreteRelationRefiner.getAbstractionPropagations();
				for (var childSymbol : childSymbols) {
					var arity = childSymbol.relation().arity();
					var input = new int[arity];
					Arrays.fill(input, -1);
					var argumentMapping = childSymbol.argumentMapping();
					for (int i = 0; i < childSymbol.argumentMapping().length; i++) {
						if (argumentMapping[i] != -1) {
							input[argumentMapping[i]] = activation.get(i);
						}
					}

					int nodeCount = reasoningAdapter.getNodeCount();
					backtrack(nodeCount, input.length, 0, input, new int[arity], tuple -> {
						Entry<PartialSymbol<?, ?>, Tuple> entry = new SimpleEntry<>(childSymbol.relation(), tuple);
						if (symbolsToFix.add(entry)) {
							newItems.push(entry);
						}
					});
				}
			}
		}

		for (var symbolToFix : symbolsToFix) {
			mergeCandidateValue(symbolToFix.getKey(), symbolToFix.getValue());
		}
	}

	private static void backtrack(int n, int k, int depth,
	                              int[] fixedArr,
	                              int[] current,
	                              Consumer<Tuple> consumer) {

		if (depth == k) {
			consumer.accept(Tuple.of(current));
			return;
		}

		int fixedVal = fixedArr[depth];
		if (fixedVal != -1) {
			current[depth] = fixedVal;
			backtrack(n, k, depth + 1, fixedArr, current, consumer);
			return;
		}

		for (int i = 0; i < n; i++) {
			current[depth] = i;
			backtrack(n, k, depth + 1, fixedArr, current, consumer);
		}
	}

	private void collectUsedPartialSymbols(BoundAction boundAction, Tuple activation,
	                                       Set<Entry<PartialSymbol<?, ?>, Tuple>> symbolsToFix) {
		var action = boundAction.getAction();
		var actionLiterals = action.getActionLiterals();
		for (int i = 0; i < actionLiterals.size(); i++) {
			var actionLiteral = actionLiterals.get(i);
			var inputAllocation = action.getInputAllocation(i);
			var input = boundAction.getInputTuple(inputAllocation, activation);
			if (actionLiteral instanceof ComputedActionLiteral<?> computedActionLiteral) {
				var valueQuery = computedActionLiteral.getValueQuery();
				var queryInput = input.map(computedActionLiteral.getArgumentMapping());
				var worklist = new Stack<Entry<Dnf, Tuple>>();
				worklist.push(new SimpleEntry<>(valueQuery.getDnf(), queryInput));
				while (!worklist.isEmpty()) {
					var current = worklist.pop();
					var symbolicParameters = current.getKey().getSymbolicParameters();
					var parameters = new ArrayList<Variable>(symbolicParameters.size());
					for (var symbolicParameter : symbolicParameters) {
						parameters.add(symbolicParameter.getVariable());
					}

					for (var clause : current.getKey().getClauses()) {
						for (var literal : clause.literals()) {
							if (literal instanceof CallLiteral callLiteral) {
								var target = callLiteral.getTarget();
								var callParams = callLiteral.getArguments();
								var mapping = new int[callParams.size()];
								for (int j = 0; j < mapping.length; j++) {
									mapping[j] = parameters.indexOf(callParams.get(j));
								}

								var arguments = current.getValue().map(mapping);
								if (target instanceof PartialRelation partialRelation) {
									symbolsToFix.add(new SimpleEntry<>(partialRelation, arguments));
								} else if (target instanceof Dnf dnf) {
									worklist.push(new SimpleEntry<>(dnf, arguments));
								}
							}
						}
					}
				}
			} else if (actionLiteral instanceof MergeActionLiteral<?, ?> mergeActionLiteral) {
				var partialSymbol = mergeActionLiteral.getPartialSymbol();
				symbolsToFix.add(new SimpleEntry<>(partialSymbol, input));
			} else if (actionLiteral instanceof JoinActionLiteral<?, ?> joinActionLiteral) {
				var partialSymbol = joinActionLiteral.getPartialSymbol();
				symbolsToFix.add(new SimpleEntry<>(partialSymbol, input));
			}
		}
	}

	private <A extends AbstractValue<A, C>, C> void mergeCandidateValue(PartialSymbol<A, C> partialSymbol, Tuple key) {
		var partialInterpretation = reasoningAdapter.getPartialInterpretation(Concreteness.CANDIDATE, partialSymbol);
		var refiner = reasoningAdapter.getRefiner(partialSymbol);
		refiner.merge(key, partialInterpretation.get(key));
	}

	private Boolean propagateForward(int stateIndex, boolean checkAccepted) {
		if (stateIndex >= traceToConcretize.states().size() - 1) {
			return null;
		}

		var forwardTransition = traceToConcretize.transitions().get(stateIndex);

		if (checkAccepted) {
			fixCandidateValuesAssumedOrUsedByTransition(forwardTransition);
		}

		if (!forwardTransition.transition().fireAction(forwardTransition.activation())) {
			return false;
		}

		return checkPropagationDiff(stateIndex, 1, checkAccepted);
	}

	private Boolean propagateBackward(int stateIndex, boolean checkAccepted) {
		if (stateIndex <= 0) {
			return null;
		}

		if (checkAccepted) {
			if (concretizationOrderManager.isEarlier(stateIndex - 1, stateIndex)) {
				return propagateForward(stateIndex - 1, true);
			}
			return null;
		}

		var forwardTransition = traceToConcretize.transitions().get(stateIndex - 1);
		var boundAction = forwardTransition.transition().getAction();
		var action = boundAction.getAction();

		// best effort "inverse"
		var inverseActionLiterals = new LinkedList<ActionLiteral>();
		List<ActionLiteral> actionLiterals = action.getActionLiterals();
		for (ActionLiteral literal : actionLiterals) {
			ActionLiteral inverseLiteral = null;
			switch (literal) {
			case MergeActionLiteral<?, ?> _, ComputedMergeActionLiteral<?, ?> _ -> {
				// since we are concretizing the trace, the two values (before and after a merge) must be the same
			}
			case JoinActionLiteral<?, ?> l -> inverseLiteral = havoc(l.getPartialSymbol(), l.getParameters());
			case ModifyActionLiteral<?, ?> l -> inverseLiteral = havoc(l.getPartialSymbol(), l.getParameters());
			default -> {
				return null;
			}
			}
			inverseActionLiterals.add(inverseLiteral);
		}

		var inverseAction = new Action(action.getParameters(), inverseActionLiterals);
		var boundInverseAction = inverseAction.bindToModel(model);

		if (!boundInverseAction.fire(forwardTransition.activation())) {
			return false;
		}

		return checkPropagationDiff(stateIndex, -1, false);
	}

	private Boolean checkPropagationDiff(int stateIndex, int direction, boolean checkAccepted) {
		if (explorationAdapter.checkExclude()) {
			return false;
		}

		boolean anyDiff = false;
		var diffCursor = model.getDiffCursor(currentVersions.get(stateIndex + direction), true);
		var symbols = model.getStore().getSymbols();
		for (var symbol : symbols) {
			var cursor = diffCursor.getCursor((Symbol<?>) symbol);
			if (cursor.move()) {
				anyDiff = true;
				break;
			}
		}

		if (!anyDiff) {
			return null;
		}

		queryAdapter.flushChanges();

		for (var listener : partialSymbolListeners) {
			listener.startListening();
		}

		model.restore(currentVersions.get(stateIndex + direction));

		for (var listener : partialSymbolListeners) {
			listener.stopListening();
		}

		var ret = checkAccepted ? true : null;
		for (var listener : partialSymbolListeners) {
			if (checkAccepted) {
				if (!listener.checkAccepted()) {
					return false;
				}
			} else {
				Boolean r = listener.mergeChanges();
				if (r != null) {
					if (!r) {
						return false;
					}
					ret = true;
				}
			}
		}

		return ret;
	}

	private <A extends AbstractValue<A, C>, C> ActionLiteral havoc(PartialSymbol<A, C> partialSymbol,
	                                                               List<NodeVariable> parameters) {
		return new JoinActionLiteral<>(partialSymbol, partialSymbol.abstractDomain().unknown(), parameters);
	}

	private boolean propagateAlongTrace(boolean checkAccepted) {
		// entry: state index + direction (true: forward)
		Stack<SimpleEntry<Integer, Boolean>> propagationEntries = new Stack<>();
		propagationEntries.push(new SimpleEntry<>(currentlyConcretized(), true));
		propagationEntries.push(new SimpleEntry<>(currentlyConcretized(), false));

		while (!propagationEntries.isEmpty()) {
			var propagationEntry = propagationEntries.pop();
			var index = propagationEntry.getKey();
			var direction = propagationEntry.getValue() ? 1 : -1;
			model.restore(currentVersions.get(index));

			Boolean result;
			if (propagationEntry.getValue()) {
				result = propagateForward(index, checkAccepted);
			} else {
				result = propagateBackward(index, checkAccepted);
			}

			if (result == null) {
				continue; // no changes
			}

			if (!result) {
				return false;
			}

			var propagationResult = propagationAdapter.propagate();
			if (propagationResult.isRejected()) {
				return false;
			}

			if (explorationAdapter.checkExclude()) {
				return false;
			}

			var propagatedVersion = model.commit();
			currentVersions.put(index + direction, propagatedVersion);

			var nextForward = new SimpleEntry<>(index + direction, true);
			if (propagationEntries.isEmpty() || !nextForward.equals(propagationEntries.peek())) {
				propagationEntries.push(nextForward);
			}
			propagationEntries.push(new SimpleEntry<>(index + direction, false));
		}

		return true;
	}

	private int compare(TraceVersion s1, TraceVersion s2) {
		return objectiveStore.getComparator().compare(s1, s2);
	}

	private void restoreToLast() {
		concretizationOrderManager.set(last.concretizedIndex);
		currentVersions.restore(last.traceVersion);
		model.restore(currentVersion());
	}

	private TraceVersion restoreToBest() {
		var bestVersion = objectiveStore.getBest();
		last = bestVersion;
		if (bestVersion != null) {
			concretizationOrderManager.set(last.concretizedIndex);
			currentVersions.restore(last.traceVersion);
			model.restore(currentVersion());
		}
		return last;
	}

	private TraceVersion restoreToRandom(Random random) {
		if (objectiveStore.getSize() == 0) {
			return null;
		}
		var randomVersion = objectiveStore.getRandom(random);
		last = randomVersion;
		if (randomVersion != null) {
			concretizationOrderManager.set(last.concretizedIndex);
			currentVersions.restore(last.traceVersion);
			model.restore(currentVersion());
		}
		return last;
	}

	private boolean hasEnoughSolution() {
		return solutionStores[currentlyConcretized()].hasEnoughSolution();
	}

	private interface ConcretizationOrderManager {
		void reset();

		void advance();

		void set(int i);

		int getCurrentConcretizedIndex();

		boolean hasMore();

		boolean isEarlier(int i, int j); // true if state index i is concretized earlier compared to j
	}

	private static class BackwardConcretizationOrder implements ConcretizationOrderManager {

		private final int size;
		private int current;

		BackwardConcretizationOrder(int size) {
			this.size = size;
			current = size - 1;
		}

		@Override
		public void reset() {
			current = size - 1;
		}

		@Override
		public void advance() {
			current--;
		}

		@Override
		public void set(int i) {
			current = i;
		}

		@Override
		public int getCurrentConcretizedIndex() {
			return current;
		}

		@Override
		public boolean hasMore() {
			return current >= 0;
		}

		@Override
		public boolean isEarlier(int i, int j) {
			return j < i;
		}
	}

	private record TraceVersion(int concretizedIndex, VersionWithObjectiveValue stateVersion, Version traceVersion) {
	}

	public record SubmitResult(boolean include, boolean accepted, ObjectiveValue objective, TraceVersion newVersion) {
	}

	public record RandomVisitResult(SubmitResult submitResult, boolean shouldRetry) {
	}

	private class PartialRelationChangeManager<A extends AbstractValue<A, C>, C> {
		private final PartialSymbol<A, C> partialSymbol;
		private final PartialInterpretation<A, C> partialPartialInterpretation;
		private final ResultSetListener<A> listener;
		private final Map<Tuple, Entry<A, A>> valuePairs = new HashMap<>();

		PartialRelationChangeManager(PartialSymbol<A, C> partialSymbol) {
			this.partialSymbol = partialSymbol;
			partialPartialInterpretation = reasoningAdapter.getPartialInterpretation(Concreteness.PARTIAL, partialSymbol);
			listener = (key, propagatedValue, originalValue) -> {
				var entry = valuePairs.get(key);
				if (entry == null) {
					valuePairs.put(key, new SimpleEntry<>(propagatedValue, originalValue));
				} else {
					entry.setValue(originalValue);
				}
			};
		}

		void startListening() {
			valuePairs.clear();
			partialPartialInterpretation.addListener(listener);
		}

		void stopListening() {
			partialPartialInterpretation.removeListener(listener);
		}

		boolean checkAccepted() {
			try {
				for (var entry : valuePairs.values()) {
					var merged = entry.getKey().meet(entry.getValue());
					if (merged.isError()) {
						return false;
					}
				}
				return true;
			} finally {
				valuePairs.clear();
			}
		}

		Boolean mergeChanges() {
			try {
				PartialInterpretationRefiner<A, C> refiner = null;
				Boolean anyChange = null;
				for (var entries : valuePairs.entrySet()) {
					var entry = entries.getValue();
					var merged = entry.getKey().meet(entry.getValue());
					if (merged.isError()) {
						return false;
					}

					if (merged.equals(entry.getValue())) {
						continue;
					}

					if (refiner == null) {
						refiner = reasoningAdapter.getRefiner(partialSymbol);
					}
					refiner.merge(entries.getKey(), merged);
					anyChange = true;
				}
				return anyChange;
			} finally {
				valuePairs.clear();
			}
		}
	}
}
