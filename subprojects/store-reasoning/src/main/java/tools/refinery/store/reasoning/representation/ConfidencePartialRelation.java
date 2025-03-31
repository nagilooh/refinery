/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.reasoning.representation;

import tools.refinery.logic.AbstractDomain;
import tools.refinery.logic.term.truthvalue.TruthValue;
import tools.refinery.logic.term.truthvalue.TruthValueConfidence;
import tools.refinery.logic.term.truthvalue.TruthValueConfidenceDomain;

public record ConfidencePartialRelation(String name, int arity)
		implements PartialSymbol<TruthValueConfidence, Boolean> {
	@Override
	public AbstractDomain<TruthValueConfidence, Boolean> abstractDomain() {
		return TruthValueConfidenceDomain.INSTANCE;
	}

	@Override
	public TruthValueConfidence defaultValue() {
		return new TruthValueConfidence(TruthValue.FALSE, 0.0);
	}

	@Override
	public boolean equals(Object o) {
		return this == o;
	}

	@Override
	public int hashCode() {
		// Compare by identity to make hash table look-ups more efficient.
		return System.identityHashCode(this);
	}

	@Override
	public String toString() {
		return "%s/%d".formatted(name, arity);
	}
}
