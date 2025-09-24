package tools.refinery.generator;

import tools.refinery.generator.impl.CancellableCancellationToken;
import tools.refinery.generator.impl.ManualExplorationImpl;
import tools.refinery.language.model.problem.Problem;
import tools.refinery.language.semantics.TracedException;
import tools.refinery.store.dse.propagation.PropagationAdapter;
import tools.refinery.store.dse.transition.DesignSpaceExplorationAdapter;
import tools.refinery.store.model.ModelStore;
import tools.refinery.store.query.interpreter.QueryInterpreterAdapter;
import tools.refinery.store.reasoning.ReasoningAdapter;
import tools.refinery.store.reasoning.interpretation.PartialNeighborhoodCalculator;
import tools.refinery.store.reasoning.literal.Concreteness;
import tools.refinery.store.reasoning.translator.TranslationException;
import tools.refinery.store.statecoding.StateCodeCalculatorFactory;
import tools.refinery.store.statecoding.StateCoderAdapter;
import tools.refinery.store.statecoding.neighborhood.NeighborhoodCalculator;
import tools.refinery.visualization.ModelVisualizerAdapter;
import tools.refinery.visualization.internal.FileFormat;

import java.util.Collection;
import java.util.Set;

public final class ManualExplorationFactory extends ModelFacadeFactory<ManualExplorationFactory> {
	private boolean debugPartialInterpretations;

	private boolean partialInterpretationBasedNeighborhoods;

	private int stateCoderDepth = NeighborhoodCalculator.DEFAULT_DEPTH;

	public ManualExplorationFactory() {
		keepShadowPredicates(false);
	}
	@Override
	protected ManualExplorationFactory getSelf() {
		return this;
	}

	public ManualExplorationFactory debugPartialInterpretations(boolean debugPartialInterpretations) {
		this.debugPartialInterpretations = debugPartialInterpretations;
		return this;
	}

	public ManualExplorationFactory partialInterpretationBasedNeighborhoods(
			boolean partialInterpretationBasedNeighborhoods) {
		this.partialInterpretationBasedNeighborhoods = partialInterpretationBasedNeighborhoods;
		return this;
	}

	public ManualExplorationFactory stateCoderDepth(int stateCoderDepth) {
		this.stateCoderDepth = stateCoderDepth;
		return this;
	}

	public ModelExplorer tryCreateGenerator(Problem problem) {
		var initializer = createModelInitializer();
		try {
			initializer.readProblem(problem);
		} catch (TracedException e) {
			throw getDiagnostics().wrapTracedException(e, problem);
		}
		checkCancelled();
		var cancellationToken = new CancellableCancellationToken(getCancellationToken());
		var storeBuilder = ModelStore.builder()
				.cancellationToken(cancellationToken)
				.with(QueryInterpreterAdapter.builder())
				.with(ModelVisualizerAdapter.builder()
						.withOutputPath("test_output")
						.withFormat(FileFormat.DOT)
						.withFormat(FileFormat.SVG)
						.saveStates()
						.saveDesignSpace())
				.with(PropagationAdapter.builder())
				.with(StateCoderAdapter.builder()
						.stateCodeCalculatorFactory(getStateCodeCalculatorFactory()))
				.with(DesignSpaceExplorationAdapter.builder())
				.with(ReasoningAdapter.builder()
						.requiredInterpretations(getRequiredInterpretations()));
		try {
			initializer.configureStoreBuilder(storeBuilder);
		} catch (TranslationException e) {
			throw getDiagnostics().wrapTranslationException(e, initializer.getProblemTrace());
		} catch (TracedException e) {
			throw getDiagnostics().wrapTracedException(e, problem);
		}
		return new ManualExplorationImpl(createFacadeArgs(initializer, storeBuilder));
	}

	public ModelExplorer createGenerator(Problem problem) {
		var generator = tryCreateGenerator(problem);
		generator.throwIfInitializationFailed();
		return generator;
	}

	private Collection<Concreteness> getRequiredInterpretations() {
		return Set.of(Concreteness.PARTIAL, Concreteness.CANDIDATE);
	}

	private StateCodeCalculatorFactory getStateCodeCalculatorFactory() {
		return PartialNeighborhoodCalculator.factory(Concreteness.PARTIAL, stateCoderDepth);
	}
}
