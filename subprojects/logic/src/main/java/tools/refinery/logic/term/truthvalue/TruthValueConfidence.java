/*
 * SPDX-FileCopyrightText: 2023-2024 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.logic.term.truthvalue;

import org.jetbrains.annotations.Nullable;
import tools.refinery.logic.AbstractValue;

public class TruthValueConfidence implements AbstractValue<TruthValueConfidence, Boolean> {
	public static final TruthValueConfidence TRUE = new TruthValueConfidence(TruthValue.TRUE, 1.0);
	public static final TruthValueConfidence FALSE = new TruthValueConfidence(TruthValue.FALSE, 0.0);
	public static final TruthValueConfidence ERROR = new TruthValueConfidence(TruthValue.ERROR, Double.NaN);

	private final TruthValue truthValue;
	private final double confidence;

	public TruthValueConfidence(TruthValue truthValue, Double confidence) {
		this.truthValue = truthValue;
		this.confidence = confidence;
	}

	public double getConfidence() {
		return confidence;
	}

	public TruthValue getTruthValue() {
		return truthValue;
	}

	@Override
	public @Nullable Boolean getConcrete() {
		return truthValue.getConcrete();
	}

	@Override
	public boolean isConcrete() {
		return truthValue.isConcrete();
	}

	@Override
	public @Nullable Boolean getArbitrary() {
		return truthValue.getArbitrary();
	}

	@Override
	public boolean isError() {
		return truthValue.isError();
	}

	@Override
	public TruthValueConfidence join(TruthValueConfidence other) {
		// TODO: This is definitely not correct
		return new TruthValueConfidence(truthValue.join(other.getTruthValue()), Math.max(this.confidence, other.getConfidence()));
	}

	@Override
	public TruthValueConfidence meet(TruthValueConfidence other) {
		// TODO: This is definitely not correct
		return new TruthValueConfidence(truthValue.meet(other.getTruthValue()), Math.min(this.confidence, other.getConfidence()));
	}

	public boolean must() {
		return truthValue.must();
	}

	public boolean may() {
		return truthValue.may();
	}

	@Override
	public boolean equals(Object other) {
		if (!(other instanceof TruthValueConfidence)) {
			return false;
		}

		return truthValue.equals(((TruthValueConfidence) other).getTruthValue())
				&& confidence == ((TruthValueConfidence) other).getConfidence();
	}

	@Override
	public String toString() {
		return "TruthValueConfidence{" + "truthValue=" + truthValue + ", confidence=" + confidence + '}';
	}
}
