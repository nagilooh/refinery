/*
 * SPDX-FileCopyrightText: 2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.dse.transition.statespace.internal;

import tools.refinery.store.dse.transition.ObjectiveValues;
import tools.refinery.store.dse.transition.VersionWithObjectiveValue;
import tools.refinery.store.dse.transition.objectives.Objective;
import tools.refinery.store.dse.transition.statespace.ObjectivePriorityQueue;

import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;
import java.util.Random;

public class ObjectivePriorityQueueImpl implements ObjectivePriorityQueue {
	public static final Comparator<VersionWithObjectiveValue> c1 = Comparator.comparingDouble(o -> ((ObjectiveValues.ObjectiveValue1) o.objectiveValue()).value0());
	// TODO: support multi objective!
	final Comparator<VersionWithObjectiveValue> comparator;
	final PriorityQueue<VersionWithObjectiveValue> priorityQueue;

	public ObjectivePriorityQueueImpl(List<Objective> objectives) {

		if(objectives.isEmpty()) {
			throw new IllegalStateException("No objectives have been created");
		}
		if(objectives.size() == 1) {
			this.comparator = c1;
		}
		else if(objectives.size() == 2) {
			var paretoComparator = new ParetoComparator<VersionWithObjectiveValue>();
			paretoComparator.add(Comparator.comparingDouble(o -> ((ObjectiveValues.ObjectiveValue2) o.objectiveValue()).value0()));
			paretoComparator.add(Comparator.comparingDouble(o -> ((ObjectiveValues.ObjectiveValue2) o.objectiveValue()).value1()));
			this.comparator = paretoComparator;
		}
		else {
			var paretoComparator = new ParetoComparator<VersionWithObjectiveValue>();
			for(var i = 0; i < objectives.size(); i++) {
				int finalI = i;
				paretoComparator.add(Comparator.comparingDouble(o -> o.objectiveValue().get(finalI)));
			}
			this.comparator = paretoComparator;


		}
		this.priorityQueue = new PriorityQueue<>(comparator);
	}

	public ObjectivePriorityQueueImpl(Comparator<VersionWithObjectiveValue> comparator) {
		this.comparator = comparator;
		this.priorityQueue = new PriorityQueue<>(comparator);
	}

	@Override
	public Comparator<VersionWithObjectiveValue> getComparator() {
		return this.comparator;
	}

	@Override
	public synchronized void submit(VersionWithObjectiveValue versionWithObjectiveValue) {
		priorityQueue.add(versionWithObjectiveValue);
	}

	@Override
	public synchronized void remove(VersionWithObjectiveValue versionWithObjectiveValue) {
		priorityQueue.remove(versionWithObjectiveValue);
	}

	@Override
	public synchronized int getSize() {
		return priorityQueue.size();
	}

	@Override
	public synchronized VersionWithObjectiveValue getBest() {
		return priorityQueue.peek();
	}

	@Override
	public synchronized VersionWithObjectiveValue getRandom(Random random) {
		int randomPosition = random.nextInt(getSize());
		for (VersionWithObjectiveValue entry : this.priorityQueue) {
			if (randomPosition-- == 0) {
				return entry;
			}
		}
		throw new IllegalStateException("The priority queue is inconsistent!");
	}
}
