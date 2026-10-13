package tools.refinery.measurement;

import java.io.IOException;

public interface MeasurementCase {
	void init(int size, boolean visualize) throws IOException;
	MeasurementResult run(int steps);
	void finish();
}
