/*
 * SPDX-FileCopyrightText: 2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.dse.transition.statespace.internal;

import tools.refinery.store.dse.transition.ObjectiveValues.ObjectiveValue1;
import tools.refinery.store.dse.transition.VersionWithObjectiveValue;
import tools.refinery.store.dse.transition.objectives.Objective;
import tools.refinery.store.dse.transition.statespace.ObjectivePriorityQueue;

import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Random;
import java.util.function.Predicate;

public class ObjectivePriorityQueueImpl<V> implements ObjectivePriorityQueue<V> {
	public static final Comparator<VersionWithObjectiveValue> c1 = Comparator.comparingDouble(o -> ((ObjectiveValue1) o.objectiveValue()).value0());

	public static ObjectivePriorityQueueImpl<VersionWithObjectiveValue> of(List<Objective> objectives) {
		if (objectives.size() != 1) {
			throw new UnsupportedOperationException("Only single objective comparator is implemented currently!");
		}
		return new ObjectivePriorityQueueImpl<>(c1);
	}

	// TODO: support multi objective!

	private final Comparator<V> comparator;
	private final PriorityQueue<V> priorityQueue;

	public ObjectivePriorityQueueImpl(Comparator<V> comparator) {
		this.comparator = comparator;
		this.priorityQueue = new PriorityQueue<>(comparator);
	}

	@Override
	public Comparator<V> getComparator() {
		return comparator;
	}

	@Override
	public synchronized void submit(V version) {
		priorityQueue.add(version);
	}

	@Override
	public synchronized void remove(V version) {
		priorityQueue.remove(version);
	}

	@Override
	public synchronized void removeIf(Predicate<V> predicate) {
		priorityQueue.removeIf(predicate);
	}

	@Override
	public synchronized int getSize() {
		return priorityQueue.size();
	}

	@Override
	public synchronized V getBest() {
		return priorityQueue.peek();
	}

	@Override
	public synchronized V getRandom(Random random) {
		int randomPosition = random.nextInt(getSize());
		for (V entry : this.priorityQueue) {
			if (randomPosition-- == 0) {
				return entry;
			}
		}
		throw new IllegalStateException("The priority queue is inconsistent!");
	}
}
