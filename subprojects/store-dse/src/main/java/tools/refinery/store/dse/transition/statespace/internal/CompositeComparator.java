/*
 * SPDX-FileCopyrightText: 2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.dse.transition.statespace.internal;

import tools.refinery.store.dse.transition.VersionWithObjectiveValue;

import java.util.Comparator;

public class CompositeComparator implements Comparator<VersionWithObjectiveValue> {

	@Override
	public int compare(VersionWithObjectiveValue o1, VersionWithObjectiveValue o2) {
		var objectiveValue1 = o1.objectiveValue();
		var objectiveValue2 = o2.objectiveValue();
		var value1 = 0.0;
		var value2 = 0.0;
		if (o1.objectiveValue().getSize() != o2.objectiveValue().getSize()) {
			throw new IllegalStateException("Comparing versions with different number of objective values");
		}
		for (int i = 0; i < o1.objectiveValue().getSize(); i++) {
			value1 += objectiveValue1.get(i);
			value2 += objectiveValue2.get(i);
		}
		return Double.compare(value1, value2);
	}
}
