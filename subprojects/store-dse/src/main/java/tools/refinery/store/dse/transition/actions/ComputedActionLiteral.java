/*
 * SPDX-FileCopyrightText: 2026 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.dse.transition.actions;

import org.jetbrains.annotations.Nullable;
import tools.refinery.logic.dnf.AnyQuery;
import tools.refinery.logic.dnf.FunctionalQuery;
import tools.refinery.logic.term.NodeVariable;

import java.util.LinkedHashMap;
import java.util.List;

public abstract class ComputedActionLiteral<T> extends AbstractActionLiteral {
	protected final List<NodeVariable> parameters;
	protected final FunctionalQuery<T> valueQuery;
	protected final List<NodeVariable> arguments;
	protected final List<NodeVariable> inputVariables;
	protected final int @Nullable [] parameterMapping;
	protected final int @Nullable [] argumentMapping;

	public ComputedActionLiteral(List<NodeVariable> parameters,
									FunctionalQuery<T> valueQuery, List<NodeVariable> arguments) {
		if (valueQuery.arity() != arguments.size()) {
			throw new IllegalArgumentException("Expected %d arguments for query %s, got %d instead"
					.formatted(valueQuery.arity(), valueQuery, arguments.size()));
		}
		this.parameters = parameters;
		this.arguments = arguments;
		this.valueQuery = valueQuery;
		var allocation = new LinkedHashMap<NodeVariable, Integer>();
		var theParameterMapping = mapVariables(parameters, allocation);
		var theArgumentMapping = mapVariables(arguments, allocation);
		inputVariables = List.copyOf(allocation.sequencedKeySet());
		parameterMapping = isIdentity(theParameterMapping) ? null : theParameterMapping;
		argumentMapping = isIdentity(theArgumentMapping) ? null : theArgumentMapping;
	}

	public int @Nullable [] getArgumentMapping() {
		return argumentMapping;
	}

	private static int[] mapVariables(List<NodeVariable> variables, LinkedHashMap<NodeVariable, Integer> allocation) {
		int size = variables.size();
		var mapping = new int[size];
		for (int i = 0; i < size; i++) {
			mapping[i] = allocation.computeIfAbsent(variables.get(i), ignored -> allocation.size());
		}
		return mapping;
	}

	private boolean isIdentity(int[] mapping) {
		int length = mapping.length;
		if (length != inputVariables.size()) {
			return false;
		}
		for (int i = 0; i < length; i++) {
			if (mapping[i] != i) {
				return false;
			}
		}
		return true;
	}

	public List<NodeVariable> getParameters() {
		return parameters;
	}

	public List<NodeVariable> getArguments() {
		return arguments;
	}

	public FunctionalQuery<T> getValueQuery() {
		return valueQuery;
	}

	@Override
	public List<NodeVariable> getInputVariables() {
		return inputVariables;
	}

	@Override
	public List<NodeVariable> getOutputVariables() {
		return List.of();
	}

	@Override
	public List<AnyQuery> getQueries() {
		return List.of(valueQuery);
	}

	@Override
	public boolean isDynamic() {
		return true;
	}
}


