package hu.bme.mit.trainbenchmark.generator.scalable;

import java.io.IOException;

import hu.bme.mit.trainbenchmark.constants.ModelConstants;
import hu.bme.mit.trainbenchmark.generator.ModelSerializer;

public class RouteSensorError extends GenerationError{
	Object route;
	Object sensor;
	
	public RouteSensorError(Object route, Object sensor) {
		super();
		this.route = route;
		this.sensor = sensor;
	}

	@Override
	void apply(ModelSerializer serializer) throws IOException {
		serializer.removeEdge(ModelConstants.REQUIRES, route, sensor);
	}
}
