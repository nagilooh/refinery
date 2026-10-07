package tools.refinery.measurement;

import tools.refinery.store.dse.transition.Transformation;
import tools.refinery.store.transition.system.statespace.Transition;
import tools.refinery.store.tuple.Tuple;

public record Activation(Transition transformation, Tuple tuple, int transformationIndex, int tupleIndex) {
    @Override
    public String toString() {
        return String.format("%s%s%n", transformation.toString(), tuple);
    }

    public boolean fire() {
        return transformation.fireAction(tuple);
    }
}
