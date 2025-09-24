package tools.refinery.store.dse.transition.statespace.internal;

import tools.refinery.store.dse.transition.VersionWithObjectiveValue;

public record Transition(VersionWithObjectiveValue from, VersionWithObjectiveValue to, int transformation,
						 int activation) {
}
