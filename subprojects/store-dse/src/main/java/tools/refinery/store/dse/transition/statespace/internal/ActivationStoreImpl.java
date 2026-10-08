/*
 * SPDX-FileCopyrightText: 2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.dse.transition.statespace.internal;

import tools.refinery.store.dse.transition.statespace.ActivationStore;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.function.BiFunction;
import java.util.function.Consumer;

public class ActivationStoreImpl<R, V> implements ActivationStore<V> {
	private final List<R> transformations;
	private final BiFunction<R, Integer, Double> weightProvider;
	private final Consumer<V> actionWhenAllActivationVisited;
	private final Map<V, List<ActivationStoreEntry>> versionToActivations;

	public ActivationStoreImpl(List<R> transformations, BiFunction<R, Integer, Double> weightProvider,
							   Consumer<V> actionWhenAllActivationVisited) {
		this.transformations = transformations;
		this.weightProvider = weightProvider;
		this.actionWhenAllActivationVisited = actionWhenAllActivationVisited;
		versionToActivations = new HashMap<>();
	}

	public synchronized VisitResult markNewAsVisited(V to, int[] emptyEntrySizes) {
		boolean[] successful = new boolean[]{false};
		var entries = versionToActivations.computeIfAbsent(to, x -> {
			successful[0] = true;
			List<ActivationStoreEntry> result = new ArrayList<>(emptyEntrySizes.length);
			for (int emptyEntrySize : emptyEntrySizes) {
				result.add(ActivationStoreEntry.create(emptyEntrySize));
			}
			return result;
		});
		boolean hasMore = false;
		for (var entry : entries) {
			if (entry.getNumberOfUnvisitedActivations() > 0) {
				hasMore = true;
				break;
			}
		}
		if (!hasMore) {
			actionWhenAllActivationVisited.accept(to);
		}
		return new VisitResult(successful[0], hasMore, -1, -1);
	}

	public synchronized VisitResult visitActivation(V from, int transformationIndex, int activationIndex) {
		var entries = versionToActivations.get(from);
		var entry = entries.get(transformationIndex);
		final int unvisited = entry.getNumberOfUnvisitedActivations();

		final boolean successfulVisit = unvisited > 0;
		final boolean hasMoreInActivation = unvisited > 1;
		final boolean hasMore;
		final int transformation;
		final int activation;

		if (successfulVisit) {
			transformation = transformationIndex;
			activation = entry.getAndAddActivationAfter(activationIndex);

		} else {
			transformation = -1;
			activation = -1;
		}

		if (!hasMoreInActivation) {
			boolean hasMoreInOtherTransformation = false;
			for (var e : entries) {
				if (e != entry && e.getNumberOfUnvisitedActivations() > 0) {
					hasMoreInOtherTransformation = true;
					break;
				}
			}
			hasMore = hasMoreInOtherTransformation;
		} else {
			hasMore = true;
		}

		if (!hasMore) {
			actionWhenAllActivationVisited.accept(from);
		}

		return new VisitResult(successfulVisit, hasMore, transformation, activation);
	}

	@Override
	public synchronized boolean hasUnmarkedActivation(V version) {
		var entries = versionToActivations.get(version);
		boolean hasMore = false;
		for (var entry : entries) {
			if (entry.getNumberOfUnvisitedActivations() > 0) {
				hasMore = true;
				break;
			}
		}
		return hasMore;
	}

	@Override
	public synchronized VisitResult getRandomAndMarkAsVisited(V version, Random random) {
		var entries = versionToActivations.get(version);

		var weights = new double[entries.size()];
		double totalWeight = 0;
		int numberOfAllUnvisitedActivations = 0;
		for (int i = 0; i < weights.length; i++) {
			var entry = entries.get(i);
			var rule = transformations.get(i);
			int unvisited = entry.getNumberOfUnvisitedActivations();
			double weight = weightProvider.apply(rule, unvisited); // rule.getWeight(unvisited);
			weights[i] = weight;
			totalWeight += weight;
			numberOfAllUnvisitedActivations += unvisited;
		}

		if (numberOfAllUnvisitedActivations == 0) {
			this.actionWhenAllActivationVisited.accept(version);
			return new VisitResult(false, false, -1, -1);
		}

		double offset = random.nextDouble(totalWeight);
		int transformation = 0;
		for (; transformation < entries.size(); transformation++) {
			double weight = weights[transformation];
			if (weight > 0 && offset < weight) {
				var entry = entries.get(transformation);
				int activation = random.nextInt(entry.getNumberOfActivations());
				return this.visitActivation(version, transformation, activation);
			}
			offset -= weight;
		}

		throw new AssertionError("Unvisited activation %f not found".formatted(offset));
	}
}
