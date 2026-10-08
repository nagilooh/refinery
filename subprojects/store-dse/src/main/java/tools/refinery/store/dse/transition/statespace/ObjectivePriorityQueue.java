/*
 * SPDX-FileCopyrightText: 2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.dse.transition.statespace;

import tools.refinery.store.dse.transition.VersionWithObjectiveValue;
import tools.refinery.store.map.Version;

import java.util.Comparator;
import java.util.Random;
import java.util.function.Predicate;

public interface ObjectivePriorityQueue<V> {
	Comparator<V> getComparator();
	void submit(V version);
	void remove(V version);
	void removeIf(Predicate<V> predicate);
	int getSize();
	V getBest();
	V getRandom(Random random);
}
