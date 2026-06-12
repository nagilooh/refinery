package tools.refinery.store.transition.system.statespace;

import tools.refinery.store.map.Version;

public record State(FiredTransition incomingTransition, Version version, State parent, int depth) {
}
