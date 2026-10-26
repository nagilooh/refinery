/*
 * SPDX-FileCopyrightText: 2024-2026 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.language.semantics.internal;

import com.google.inject.Inject;
import tools.refinery.language.model.problem.*;
import tools.refinery.language.validation.ActionTargetCollector;

import java.util.*;

public class MutableRelationCollector {
	private final Set<Relation> mutablePredicates = new HashSet<>();

	@Inject
	private ActionTargetCollector actionTargetCollector;

	public boolean isMutable(Relation relation) {
		return mutablePredicates.contains(relation);
	}

	public void collectMutableRelations(Collection<Problem> problems) {
		problems.forEach(this::collectMutableRelations);
	}

	public void collectMutableRelations(Problem problem) {
		mutablePredicates.addAll(actionTargetCollector.getActionTargets(problem));
		for (var statement : problem.getStatements()) {
			switch (statement) {
			case ClassDeclaration classDeclaration -> collectMutableRelations(classDeclaration);
			case PredicateDefinition predicateDefinition -> collectMutableRelations(predicateDefinition);
			case RuleDefinition ruleDefinition -> collectMutableRelations(ruleDefinition);
			default -> {
				// No mutable predicates to collect (actions of {@code RuleDefinition} instances have already been
				// processed by {@link ActionTargetCollector}).
			}
			}
		}
	}

	private void collectMutableRelations(RuleDefinition ruleDefinition) {
		mutablePredicates.add(ruleDefinition.getPreconditionPredicate());
		for (var parameter : ruleDefinition.getParameters()) {
			var parameterType = parameter.getParameterType();
			if (parameterType != null) {
				mutablePredicates.add(parameterType);
			}
		}
		for (var conjunction : ruleDefinition.getPreconditions()) {
			for (var literal : conjunction.getLiterals()) {
				if (Objects.requireNonNull(literal) instanceof Atom atom) {
					var atomRelation = atom.getRelation();
					if (Objects.requireNonNull(atomRelation) instanceof PredicateDefinition predicateDefinition) {
						makePredicateMutable(predicateDefinition);
					}
				}
				if (Objects.requireNonNull(literal) instanceof NegationExpr negation) {
					var body = negation.getBody();
					if (Objects.requireNonNull(body) instanceof Atom atom) {
						var atomRelation = atom.getRelation();
						if (Objects.requireNonNull(atomRelation) instanceof PredicateDefinition predicateDefinition) {
							makePredicateMutable(predicateDefinition);
						}
					}
				}
			}
		}
	}

	private void makePredicateMutable(PredicateDefinition predicateDefinition) {
		mutablePredicates.add(predicateDefinition);
		for (var body : predicateDefinition.getBodies()) {
			for (var literal : body.getLiterals()) {
				if (Objects.requireNonNull(literal) instanceof Atom atom) {
					var atomRelation = atom.getRelation();
					if (atomRelation == null) {
						continue;
					}
					if (atomRelation instanceof PredicateDefinition nestedPredicateDefinition) {
						makePredicateMutable(nestedPredicateDefinition);
					}
				}
				if (Objects.requireNonNull(literal) instanceof NegationExpr negation) {
					var negationBody = negation.getBody();
					if (Objects.requireNonNull(negationBody) instanceof Atom atom) {
						var atomRelation = atom.getRelation();
						if (Objects.requireNonNull(atomRelation) instanceof PredicateDefinition negatedPredicateDefinition) {
							makePredicateMutable(negatedPredicateDefinition);
						}
					}
				}
			}
		}
	}

	private void collectMutableRelations(ClassDeclaration classDeclaration) {
		for (var referenceDeclaration : classDeclaration.getFeatureDeclarations()) {
			var targetType = referenceDeclaration.getReferenceType();
			if (targetType != null) {
				mutablePredicates.add(targetType);
			}
			mutablePredicates.addAll(referenceDeclaration.getSuperSets());
		}
	}

	private void collectMutableRelations(PredicateDefinition predicateDefinition) {
		for (var parameter : predicateDefinition.getParameters()) {
			var parameterType = parameter.getParameterType();
			if (parameterType != null) {
				mutablePredicates.add(parameterType);
			}
		}
		mutablePredicates.addAll(predicateDefinition.getSuperSets());
	}
}
