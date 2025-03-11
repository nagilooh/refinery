/*
 * SPDX-FileCopyrightText: 2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.reasoning.actions;

import tools.refinery.logic.AbstractValue;
import tools.refinery.logic.term.NodeVariable;
import tools.refinery.logic.term.truthvalue.TruthValueConfidence;
import tools.refinery.store.reasoning.representation.ConfidencePartialRelation;
import tools.refinery.store.reasoning.representation.PartialRelation;
import tools.refinery.store.reasoning.representation.PartialSymbol;
import tools.refinery.logic.term.truthvalue.TruthValue;

import java.util.List;

public final class PartialActionLiterals {
	private PartialActionLiterals() {
		throw new IllegalStateException("This is a static utility class and should not be instantiated directly");
	}

	public static <A extends AbstractValue<A, C>, C> MergeActionLiteral<A, C> merge(
			PartialSymbol<A, C> partialSymbol, A value, NodeVariable... parameters) {
		return new MergeActionLiteral<>(partialSymbol, value, List.of(parameters));
	}

	public static MergeActionLiteral<TruthValue, Boolean> add(PartialRelation partialRelation,
															  NodeVariable... parameters) {
		return merge(partialRelation, TruthValue.TRUE, parameters);
	}

	public static MergeActionLiteral<TruthValueConfidence, Boolean> add(ConfidencePartialRelation partialRelation,
															  NodeVariable... parameters) {
		return merge(partialRelation, new TruthValueConfidence(TruthValue.TRUE, 1.0), parameters);
	}

	public static MergeActionLiteral<TruthValue, Boolean> remove(PartialRelation partialRelation,
																 NodeVariable... parameters) {
		return merge(partialRelation, TruthValue.FALSE, parameters);
	}

	public static MergeActionLiteral<TruthValueConfidence, Boolean> remove(ConfidencePartialRelation partialRelation,
																 NodeVariable... parameters) {
		return merge(partialRelation, new TruthValueConfidence(TruthValue.FALSE, 0.0), parameters);
	}

	public static FocusActionLiteral focus(NodeVariable parent, NodeVariable child) {
		return new FocusActionLiteral(parent, child);
	}
}
