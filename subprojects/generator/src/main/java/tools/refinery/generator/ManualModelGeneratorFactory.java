/*
 * SPDX-FileCopyrightText: 2023-2024 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.generator;

import tools.refinery.generator.impl.CancellableCancellationToken;
import tools.refinery.generator.impl.ManualModelGeneratorImpl;
import tools.refinery.generator.impl.ModelGeneratorImpl;
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

// This class is used as a fluent builder, so it's not necessary to use the return value of all of its methods.
@SuppressWarnings("UnusedReturnValue")
public final class ManualModelGeneratorFactory extends ModelFacadeFactory<ManualModelGeneratorFactory> {
	private boolean debugPartialInterpretations;

	private boolean partialInterpretationBasedNeighborhoods;

	private int stateCoderDepth = NeighborhoodCalculator.DEFAULT_DEPTH;

	public ManualModelGeneratorFactory() {
		keepShadowPredicates(false);
	}

	@Override
	protected ManualModelGeneratorFactory getSelf() {
		return this;
	}

	public ManualModelGeneratorFactory debugPartialInterpretations(boolean debugPartialInterpretations) {
		this.debugPartialInterpretations = debugPartialInterpretations;
		return this;
	}

	public ManualModelGeneratorFactory partialInterpretationBasedNeighborhoods(
			boolean partialInterpretationBasedNeighborhoods) {
		this.partialInterpretationBasedNeighborhoods = partialInterpretationBasedNeighborhoods;
		return this;
	}

	public ManualModelGeneratorFactory stateCoderDepth(int stateCoderDepth) {
		this.stateCoderDepth = stateCoderDepth;
		return this;
	}

	public ManualModelGenerator tryCreateGenerator(Problem problem) {
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
		return new ManualModelGeneratorImpl(createConcreteFacadeArgs(initializer, storeBuilder), cancellationToken);
	}

	public ManualModelGenerator createGenerator(Problem problem) {
		var generator = tryCreateGenerator(problem);
		generator.throwIfInitializationFailed();
		return generator;
	}

	private Collection<Concreteness> getRequiredInterpretations() {
		return debugPartialInterpretations || partialInterpretationBasedNeighborhoods ?
				Set.of(Concreteness.PARTIAL, Concreteness.CANDIDATE) :
				Set.of(Concreteness.CANDIDATE);
	}

	private StateCodeCalculatorFactory getStateCodeCalculatorFactory() {
		return partialInterpretationBasedNeighborhoods ?
				PartialNeighborhoodCalculator.factory(Concreteness.PARTIAL, stateCoderDepth) :
				NeighborhoodCalculator.factory(stateCoderDepth);
	}
}
