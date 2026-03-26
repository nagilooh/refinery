package tools.refinery.generator.gui;

import org.jspecify.annotations.NonNull;
import tools.refinery.store.dse.transition.Transformation;
import tools.refinery.store.tuple.Tuple;

public record Activation(Transformation transformation, Tuple tuple) {
	@Override
	public @NonNull String toString() {
		return String.format("%s%s%n", transformation.getDefinition().rule().getName(), tuple);
	}

	public boolean fire() {
		return transformation.fireActivation(tuple);
	}
}
