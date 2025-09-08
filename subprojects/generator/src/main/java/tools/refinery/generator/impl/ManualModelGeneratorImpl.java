/*
 * SPDX-FileCopyrightText: 2023-2024 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.generator.impl;

import tools.refinery.generator.GeneratorResult;
import tools.refinery.generator.GeneratorTimeoutException;
import tools.refinery.generator.ManualModelGenerator;
import tools.refinery.generator.ModelGenerator;
import tools.refinery.language.model.problem.Problem;
import tools.refinery.logic.AbstractValue;
import tools.refinery.store.dse.propagation.PropagationRejectedException;
import tools.refinery.store.dse.strategy.BestFirstStoreManager;
import tools.refinery.store.dse.transition.statespace.SolutionStore;
import tools.refinery.store.map.Version;
import tools.refinery.store.reasoning.interpretation.PartialInterpretation;
import tools.refinery.store.reasoning.representation.PartialSymbol;

import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class ManualModelGeneratorImpl extends ModelGeneratorImpl implements ManualModelGenerator {

	public ManualModelGeneratorImpl(Args args, CancellableCancellationToken cancellationToken) {
		super(args, cancellationToken);
	}

	@Override
	public void manualStep(int numberOfSteps) {
		if (cancellationToken.isCancelled()) {
			throw new IllegalStateException("Model generation was previously cancelled");
		}
		var bestFirst = new BestFirstStoreManager(getModelStore(), maxNumberOfSolutions);
		try {
			bestFirst.manualStep(initialVersion, numberOfSteps);
		} catch (PropagationRejectedException e) {
			// Fatal propagation error.
			throw getDiagnostics().wrapPropagationRejectedException(e, getProblemTrace());
		}
	}
}
