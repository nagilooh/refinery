/*
 * SPDX-FileCopyrightText: 2023-2024 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.generator.impl;

import tools.refinery.generator.ManualModelGenerator;
import tools.refinery.store.dse.propagation.PropagationRejectedException;
import tools.refinery.store.dse.strategy.ManualStoreManager;

public class ManualModelGeneratorImpl extends ModelGeneratorImpl implements ManualModelGenerator {

	public ManualModelGeneratorImpl(Args args, CancellableCancellationToken cancellationToken) {
		super(args, cancellationToken);
	}

	@Override
	public void manualStep() {
		if (cancellationToken.isCancelled()) {
			throw new IllegalStateException("Model generation was previously cancelled");
		}
		var manual = new ManualStoreManager(getModelStore());
		try {
			manual.manualStep(initialVersion);
		} catch (PropagationRejectedException e) {
			// Fatal propagation error.
			throw getDiagnostics().wrapPropagationRejectedException(e, getProblemTrace());
		}
	}
}
