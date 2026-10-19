package tools.refinery.measurement;

import java.io.IOException;

public interface MeasurementCase {
	void init(int size, MeasurementScenario scenario, boolean visualize) throws IOException;
	MeasurementResult run(int steps);
	void finish();
}
