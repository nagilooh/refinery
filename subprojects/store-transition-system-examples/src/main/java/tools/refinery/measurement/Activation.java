package tools.refinery.measurement;

import org.jspecify.annotations.NonNull;
import tools.refinery.store.tuple.Tuple;

public record Activation(MeasuredTransitionRule transition, Tuple tuple, int transitionIndex, int tupleIndex) {
    @Override
    public @NonNull String toString() {
        return String.format("%s%s", transition.transition().toString(), tuple);
    }

    public boolean fire() {
        return transition.fire(tuple);
    }
}
