package tools.refinery.store.transition.system.statespace;

import org.jetbrains.annotations.NotNull;
import tools.refinery.store.tuple.Tuple;

public record FiredTransition(
	Transition transition,
	Tuple activation
) {

	@Override
	public @NotNull String toString() {
		return "FiredTransition{" +
				"transition=" + transition +
				", activation=" + activation +
				'}';
	}
}
