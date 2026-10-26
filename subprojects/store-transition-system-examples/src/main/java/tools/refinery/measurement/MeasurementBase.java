package tools.refinery.measurement;

import tools.refinery.logic.dnf.RelationalQuery;
import tools.refinery.store.dse.propagation.PropagationAdapter;
import tools.refinery.store.dse.transition.DesignSpaceExplorationAdapter;
import tools.refinery.store.dse.transition.statespace.ActivationStore;
import tools.refinery.store.map.Version;
import tools.refinery.store.model.Model;
import tools.refinery.store.model.ModelStore;
import tools.refinery.store.model.ModelStoreBuilder;
import tools.refinery.store.query.ModelQueryAdapter;
import tools.refinery.store.query.ModelQueryBuilder;
import tools.refinery.store.query.resultset.ResultSet;
import tools.refinery.store.reasoning.ReasoningStoreAdapter;
import tools.refinery.store.reasoning.seed.ModelSeed;
import tools.refinery.store.statecoding.StateCoderAdapter;
import tools.refinery.store.transition.system.TransitionSystemAdapter;
import tools.refinery.store.transition.system.statespace.Transition;
import tools.refinery.store.transition.system.strategy.TransitionSystemStoreManager;
import tools.refinery.store.dse.transition.statespace.StateSpaceStore;
import tools.refinery.store.tuple.Tuple;
import tools.refinery.visualization.ModelVisualizerAdapter;
import tools.refinery.visualization.internal.FileFormat;

import java.util.*;

/**
 * Common base class for measurement classes. Extracts shared fields and behavior used by
 * the various measurement implementations (e.g. step, run, finish, transition system start,
 * activation collection).
 */
public abstract class MeasurementBase implements MeasurementCase {
    protected boolean visualize = false;

	protected List<RelationalQuery> queries = new ArrayList<>();
	protected List<MeasuredTransitionRule> measuredTransitionRules = new ArrayList<>();
	protected List<String> fireOneTransitionRules = new ArrayList<>();
	protected List<String> fireAllTransitionRules = new ArrayList<>();

	protected Map<RelationalQuery, List<Long>> queryTimes = new HashMap<>();
	protected Map<RelationalQuery, List<ResultSet<Boolean>>> queryMatches = new HashMap<>();
	protected Map<Transition, List<Long>> transitionTimes = new HashMap<>();
	protected Map<Transition, List<Long>> propagationTimes = new HashMap<>();

    protected ModelStore store;
	protected ModelQueryBuilder modelQueryBuilder;
    protected ModelQueryAdapter queryEngine;
    protected PropagationAdapter propagationAdapter;
    protected DesignSpaceExplorationAdapter dseAdapter;
    protected TransitionSystemAdapter tsAdapter;
	protected ModelSeed modelSeed;
    protected Model model;
    protected final Random random = new Random(1);
    protected Version currentVersion;
    protected ModelVisualizerAdapter modelVisualizerAdapter;
    protected StateSpaceStore stateSpaceStore;
    protected StateCoderAdapter stateCoderAdapter;
	protected ModelStoreBuilder storeBuilder = ModelStore.builder();
	protected String visualizationOutputPath = "output";

	protected void init() {
		if (visualize) {
			storeBuilder = storeBuilder.with(ModelVisualizerAdapter.builder()
					.withOutputPath(visualizationOutputPath)
					.withFormat(FileFormat.DOT)
					.withFormat(FileFormat.SVG)
					.saveStates()
					.saveDesignSpace()
			);
		}
		this.modelQueryBuilder = storeBuilder.getAdapter(ModelQueryBuilder.class);
		modelQueryBuilder.queries(queries
		);
		this.store = storeBuilder.build();
		this.model = store.getAdapter(ReasoningStoreAdapter.class).tryCreateInitialModel(modelSeed).model();
		this.queryEngine = model.getAdapter(ModelQueryAdapter.class);
		this.currentVersion = model.commit();
		this.queryEngine.flushChanges();
		this.propagationAdapter = model.getAdapter(PropagationAdapter.class);
		this.dseAdapter = model.getAdapter(DesignSpaceExplorationAdapter.class);
		this.tsAdapter = model.getAdapter(TransitionSystemAdapter.class);

		if (visualize) {
			this.modelVisualizerAdapter = model.getAdapter(ModelVisualizerAdapter.class);
			this.stateSpaceStore = model.getAdapter(DesignSpaceExplorationAdapter.class).getStateSpaceStore();
			this.stateCoderAdapter = model.getAdapter(StateCoderAdapter.class);
		}

		for (var query : queries) {
			queryTimes.put(query, new ArrayList<>());
			queryMatches.put(query, new ArrayList<>());
		}
		for (var transition : tsAdapter.getTransitions()) {
			TransitionType transitionType;
			if (fireOneTransitionRules.contains(transition.toString())) {
				transitionType = TransitionType.FIRE_ONE;
			} else if (fireAllTransitionRules.contains(transition.toString())) {
				transitionType = TransitionType.FIRE_ALL;
			} else {
				throw new RuntimeException("Missing transition type for transition: " + transition.toString());
			}
			measuredTransitionRules.add(new MeasuredTransitionRule(transition, transitionType));
			transitionTimes.put(transition, new ArrayList<>());
			propagationTimes.put(transition, new ArrayList<>());
		}
	}

	public void transformAndRecheck(int steps) {
		for (int i = 0; i < measuredTransitionRules.size(); i++) {
			var transition = measuredTransitionRules.get(i);
			for (int step = 0; step < steps; step++) {
				var activationCursor = transition.transition().getAllActivationsAsResultSet().getAll();
				List<Activation> activationList = new ArrayList<>();
				var tupleIndex = 0;
				while (activationCursor.move()) {
					var tuple = activationCursor.getKey();
					if (transition.transitionType() == TransitionType.FIRE_ALL) {
						tuple = Tuple.of(tuple.get(0));
					}
					var activation = new Activation(transition, tuple, i, tupleIndex);
//					System.out.println("Activation found: " + activation);
					activationList.add(activation);
					tupleIndex++;
				}
				if (activationList.isEmpty()) {
					System.out.println("No more activations available for transition: " + transition.transition());
					break;
				}
				var randomActivation = activationList.get((random.nextInt(activationList.size())));
				fireTransition(randomActivation);
				check();
			}
		}
	}

    /**
     * Run a number of random exploration steps using the transition system activations.
     */
	// TODO: handle transitions, where multiple rules must be fired. E.g. in the Train Benchmark,
	//  'injectSwitchMonitored'  requires deleting all edges of 'switch' to sensors.
    public MeasurementResult run(int steps) {
		if (visualize) {
			stateSpaceStore.addState(currentVersion, stateCoderAdapter.calculateModelCode(), false, false);
		}
        for (int i = 0; i < steps; i++) {
			List<Activation> activations = getActivations();
			if (activations.isEmpty()) {
				System.out.println("No more activations available, stopping exploration.");
				return MeasurementResult.NO_MORE_TRANSITIONS;
			}
			var repairActivations =
					activations.stream().filter(a -> a.transition().toString().contains("repair")).toList();
			if (!repairActivations.isEmpty()) {
				System.out.println("Repair activations available, prioritizing repair activations.");
				activations = repairActivations;
			}
            var randomActivation = activations.get((random.nextInt(activations.size())));
            fireTransition(randomActivation);
        }
		return MeasurementResult.MAX_STEPS_REACHED;
    }

    /**
     * Finish the measurement
     */
    public void finish() {
        if (visualize) {
            modelVisualizerAdapter.visualize(stateSpaceStore, renderTransitionsToAlreadyVisitedStates());
        }
        model.close();
		for (var query : queryTimes.keySet()) {
			System.out.println("Query: " + query);
			var times = queryTimes.get(query);
			var matches = queryMatches.get(query);
			for (int i = 0; i < times.size(); i++) {
				System.out.println("Step " + i + ": Time: " + times.get(i) + " ns, Matches: " + matches.get(i).size());
			}
		}
		for (var transition : tsAdapter.getTransitions()) {
			System.out.println("Transition: " + transition);
			var times = transitionTimes.get(transition);
			var propagations = propagationTimes.get(transition);
			for (int i = 0; i < times.size(); i++) {
				System.out.println("Step " + i + ": Transition Time: " + times.get(i) + " ns, Propagation Time: " + propagations.get(i) + " ns");
			}
		}
    }

    /**
     * Start transition system exploration
     */
    public void runTransitionSystem() {
        var manager = new TransitionSystemStoreManager(store);
        manager.startExploration(this.currentVersion);
    }

    /**
     * Fire a single activation
     */
    protected void fireTransition(Activation activation) {
        Version lastVersion = null;
        if (visualize) {
            lastVersion = this.currentVersion;
        }
		var transitionStartTime = System.nanoTime();
        var result = activation.fire();
		var transitionEndTime = System.nanoTime();
		transitionTimes.get(activation.transition().transition()).add(transitionEndTime - transitionStartTime);
		System.out.println("Fired activation: " + activation + ", result: " + result);
		if (!result) {
			System.out.println("Activation failed to fire: " + activation);
			return;
		}
		var propagationStartTime = System.nanoTime();
        propagationAdapter.propagate();
		var propagationEndTime = System.nanoTime();
		propagationTimes.get(activation.transition().transition()).add(propagationEndTime - propagationStartTime);
		// TODO: This should be moved to "recheck" phase
        dseAdapter.checkAccept();
        this.currentVersion = model.commit();
        if (visualize) {
            stateSpaceStore.addState(currentVersion, stateCoderAdapter.calculateModelCode(), false, false);
            stateSpaceStore.addTransition(lastVersion, this.currentVersion, new ActivationStore.VisitResult(
                    true, true, activation.transitionIndex(), activation.tupleIndex()));
        }
		// TODO: This should be measured either in the transition time or in the recheck phase.
        queryEngine.flushChanges();
    }

    /**
     * Collect all activations from the transition system adapter.
     */
    protected List<Activation> getActivations() {
        List<Activation> activationList = new ArrayList<>();
        var transformationIndex = 0;
        for (var transition : measuredTransitionRules) {
            var activationCursor = transition.transition().getAllActivationsAsResultSet().getAll();
            var tupleIndex = 0;
            while (activationCursor.move()) {
				var activation = new Activation(transition, activationCursor.getKey(), transformationIndex, tupleIndex);
//				System.out.println("Activation found: " + activation);
                activationList.add(activation);
                tupleIndex++;
            }
            transformationIndex++;
        }
        return activationList;
    }

	protected void check() {
		var recheckStartTime = System.nanoTime();
		for (var query : queryTimes.keySet()) {
			queryMatches.get(query).add(getMatches(query));
		}
		var recheckEndTime = System.nanoTime();
	}

	protected ResultSet<Boolean> getMatches(RelationalQuery query) {
		var queryStartTime = System.nanoTime();
		var resultSet = queryEngine.getResultSet(query);
		var queryEndTime = System.nanoTime();
		queryTimes.get(query).add(queryEndTime - queryStartTime);
		return resultSet;
	}

    /**
     * Subclasses control the boolean parameter passed to the model visualizer when finishing.
     */
    protected abstract boolean renderTransitionsToAlreadyVisitedStates();
}

