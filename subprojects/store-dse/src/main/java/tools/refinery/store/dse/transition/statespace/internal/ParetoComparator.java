/*
 * SPDX-FileCopyrightText: 2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.dse.transition.statespace.internal;

import java.util.Comparator;
import java.util.LinkedList;

public class ParetoComparator<T> implements Comparator<T> {
	private final LinkedList<Comparator<T>> comparators = new LinkedList<>();

	public boolean add(final Comparator<T> c) {
		return comparators.add(c);

	}

	@Override
	public int compare(T o1, T o2) {
		var reference = 0;
		for (Comparator<T> comparator : comparators) {
			var comparison = (int) Math.signum(comparator.compare(o1, o2));
			if (reference == 0) {
				reference = comparison;
			} else {
				if (comparison * reference < 0) {
					return 0;
				}
			}
		}
		return reference;
	}
}
