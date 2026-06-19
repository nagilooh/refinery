/*
 * SPDX-FileCopyrightText: 2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.reasoning.actions;

import tools.refinery.logic.AbstractValue;
import tools.refinery.logic.dnf.FunctionalQuery;
import tools.refinery.logic.term.NodeVariable;
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
		return merge(partialSymbol, value, List.of(parameters));
	}

	public static <A extends AbstractValue<A, C>, C> MergeActionLiteral<A, C> merge(
			PartialSymbol<A, C> partialSymbol, A value, List<NodeVariable> parameters) {
		return new MergeActionLiteral<>(partialSymbol, value, parameters);
	}

	public static <A extends AbstractValue<A, C>, C> ComputedMergeActionLiteral<A, C> mergeComputed(
			PartialSymbol<A, C> partialSymbol, List<NodeVariable> parameters, FunctionalQuery<A> valueQuery,
			List<NodeVariable> arguments) {
		return new ComputedMergeActionLiteral<>(partialSymbol, parameters, valueQuery, arguments);
	}

	public static MergeActionLiteral<TruthValue, Boolean> add(PartialRelation partialRelation,
															  NodeVariable... parameters) {
		return merge(partialRelation, TruthValue.TRUE, parameters);
	}

	public static MergeActionLiteral<TruthValue, Boolean> remove(PartialRelation partialRelation,
																 NodeVariable... parameters) {
		return merge(partialRelation, TruthValue.FALSE, parameters);
	}

	public static <A extends AbstractValue<A, C>, C> JoinActionLiteral<A, C> join(
			PartialSymbol<A, C> partialSymbol, A value, NodeVariable... parameters) {
		return join(partialSymbol, value, List.of(parameters));
	}

	public static <A extends AbstractValue<A, C>, C> JoinActionLiteral<A, C> join(
			PartialSymbol<A, C> partialSymbol, A value, List<NodeVariable> parameters) {
		return new JoinActionLiteral<>(partialSymbol, value, parameters);
	}

	public static JoinActionLiteral<TruthValue, Boolean> forget(PartialSymbol<TruthValue, Boolean> partialSymbol,
																NodeVariable... parameters) {
		return join(partialSymbol, TruthValue.UNKNOWN, parameters);
	}

	public static <A extends AbstractValue<A, C>, C> ModifyActionLiteral<A, C> modify(
			PartialSymbol<A, C> partialSymbol, A value, NodeVariable... parameters) {
		return modify(partialSymbol, value, List.of(parameters));
	}

	public static <A extends AbstractValue<A, C>, C> ModifyActionLiteral<A, C> modify(
			PartialSymbol<A, C> partialSymbol, A value, List<NodeVariable> parameters) {
		return new ModifyActionLiteral<>(partialSymbol, value, parameters);
	}

	public static <A extends AbstractValue<A, C>, C> ComputedModifyActionLiteral<A, C> modifyComputed(
			PartialSymbol<A, C> partialSymbol, List<NodeVariable> parameters, FunctionalQuery<A> valueQuery,
			List<NodeVariable> arguments) {
		return new ComputedModifyActionLiteral<>(partialSymbol, parameters, valueQuery, arguments);
	}

	public static FocusActionLiteral focus(NodeVariable parent, NodeVariable child) {
		return new FocusActionLiteral(parent, child);
	}
}
