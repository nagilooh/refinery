/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.visualization.statespace.internal;

import tools.refinery.store.map.Version;
import tools.refinery.store.tuple.Tuple;
import tools.refinery.visualization.statespace.VisualizationStore;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

public class VisualizationStoreImpl implements VisualizationStore {

	private final Map<Version, Integer> states = new HashMap<>();
	private final Map<Version, Integer> stateCodes = new HashMap<>();
	private int transitionCounter = 0;
	private Integer numberOfStates = 0;
	private final StringBuilder designSpaceBuilder = new StringBuilder();
	private final StringBuilder transitionsToAlreadyVisitedStatesBuilder = new StringBuilder();
	private final Function<Integer, String> nodeNameProvider;

	public VisualizationStoreImpl(Function<Integer, String> nodeNameProvider) {
		this.nodeNameProvider = nodeNameProvider;
	}

	private String getLabel(String name, Tuple activation) {
		if (nodeNameProvider == null) {
			return name + " " + activation;
		}

		var builder = new StringBuilder();
		builder.append(name);
		builder.append(" [");
		for (int i = 0; i < activation.getSize(); i++) {
			builder.append(nodeNameProvider.apply(activation.get(i)));
			if (i < activation.getSize() - 1) {
				builder.append(", ");
			}
		}
		builder.append("]");
		return builder.toString();
	}

	@Override
	public synchronized void addState(Version state, String label, Integer stateCode) {
		if (states.containsKey(state)) {
			return;
		}
		states.put(state, numberOfStates++);
		stateCodes.put(state, stateCode);
		designSpaceBuilder.append(states.get(state)).append(" [label = \"").append(states.get(state)).append(" (");
		designSpaceBuilder.append(label);
		designSpaceBuilder.append(")\"\n").append("URL=\"./").append(states.get(state)).append(".svg\"]\n");
	}

	@Override
	public synchronized void addSolution(Version state) {
		designSpaceBuilder.append(states.get(state)).append(" [peripheries = 2]\n");
	}

	@Override
	public synchronized void addTransition(Version from, Version to, String name, Tuple activation) {
		var label = getLabel(name, activation);
		addTransition(designSpaceBuilder, from, to, label, "style=solid");
	}

	@Override
	public synchronized void addTransition(Version from, int to, String name, Tuple activation) {
		var label = getLabel(name, activation);
		var toVersion = stateCodes.entrySet().stream().filter(e -> e.getValue() == to).findAny().get().getKey();
		addTransition(transitionsToAlreadyVisitedStatesBuilder, from, toVersion, label, "style=dashed, " +
				"color=\"#00000080\", fontcolor=\"#00000080\"");
	}

	private void addTransition(StringBuilder builder, Version from, Version to, String label, String style) {
		builder.append(states.get(from)).append(" -> ").append(states.get(to))
				.append(" [label=\"").append(transitionCounter++).append(": ").append(label).append("\", ")
				.append(style).append("]\n");
	}

	public synchronized StringBuilder getDesignSpaceStringBuilder(boolean includeTransitionsToAlreadyVisitedStates) {
		return includeTransitionsToAlreadyVisitedStates ?
				designSpaceBuilder.append(transitionsToAlreadyVisitedStatesBuilder) :
				designSpaceBuilder;
	}

	@Override
	public Map<Version, Integer> getStates() {
		return states;
	}
}
