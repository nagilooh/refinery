package tools.refinery.generator.gui;

import org.jspecify.annotations.NonNull;
import tools.refinery.store.dse.transition.Transformation;
import tools.refinery.store.tuple.Tuple;

import java.util.List;

public record Activation(Transformation transformation, Tuple tuple, List<String> names) {
	@Override
	public @NonNull String toString() {
		return String.format("%s%s%n", transformation.getDefinition().rule().getName(), names);
	}

	public boolean fire() {
		return transformation.fireActivation(tuple);
	}
}
