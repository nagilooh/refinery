/*
 * SPDX-FileCopyrightText: 2021-2024 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.logic.term.truthvalue;

import tools.refinery.logic.AbstractDomain;

// Singleton pattern, because there is only one domain for truth values.
@SuppressWarnings("squid:S6548")
public final class TruthValueConfidenceDomain implements AbstractDomain<TruthValueConfidence, Boolean> {
	public static final TruthValueConfidenceDomain INSTANCE = new TruthValueConfidenceDomain();

	private TruthValueConfidenceDomain() {
	}

	@Override
	public Class<TruthValueConfidence> abstractType() {
		return TruthValueConfidence.class;
	}

	@Override
	public Class<Boolean> concreteType() {
		return Boolean.class;
	}

	@Override
	public TruthValueConfidence unknown() {
		return new TruthValueConfidence(TruthValue.UNKNOWN, 0.5);
	}

	@Override
	public TruthValueConfidence error() {
		return new TruthValueConfidence(TruthValue.ERROR, 0.0);
	}

	@Override
	public TruthValueConfidence toAbstract(Boolean concreteValue) {
		return new TruthValueConfidence(TruthValue.toTruthValue(concreteValue), Boolean.TRUE.equals(concreteValue) ? 1.0 : 0.0);
	}
}
