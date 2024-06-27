package hu.bme.mit.trainbenchmark.generator.scalable;

import java.io.IOException;

import hu.bme.mit.trainbenchmark.constants.ModelConstants;
import hu.bme.mit.trainbenchmark.generator.ModelSerializer;

public class SwitchMonitorError extends GenerationError{
	Object sw;
	Object sensor;
	
	public SwitchMonitorError(Object sw, Object sensor) {
		super();
		this.sw = sw;
		this.sensor = sensor;
	}

	@Override
	void apply(ModelSerializer serializer) throws IOException {
		serializer.removeEdge(ModelConstants.MONITORED_BY, sw, sensor);
	}
}
