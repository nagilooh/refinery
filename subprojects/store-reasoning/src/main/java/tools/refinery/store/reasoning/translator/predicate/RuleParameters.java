/*
 * SPDX-FileCopyrightText: 2025 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.reasoning.translator.predicate;

import tools.refinery.logic.literal.Literal;
import tools.refinery.logic.term.NodeVariable;
import tools.refinery.logic.term.truthvalue.TruthValue;
import tools.refinery.store.reasoning.representation.PartialRelation;

import java.util.List;

public record RuleParameters(String propagationName, List<NodeVariable> ruleParameters, List<Literal> precondition,
							 PartialRelation partialRelationTarget, TruthValue toMerge,
							 List<NodeVariable> actionParameters) {
}
