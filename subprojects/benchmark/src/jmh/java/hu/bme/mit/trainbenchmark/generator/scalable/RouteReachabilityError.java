package hu.bme.mit.trainbenchmark.generator.scalable;

import static hu.bme.mit.trainbenchmark.constants.ModelConstants.CONNECTS_TO;

import java.io.IOException;

import hu.bme.mit.trainbenchmark.generator.ModelSerializer;

public class RouteReachabilityError extends GenerationError {
	Object from;
	Object to;
	
	public RouteReachabilityError(Object from, Object to) {
		super();
		this.from = from;
		this.to = to;
	}

	@Override
	void apply(ModelSerializer serializer) throws IOException {
		serializer.removeEdge(CONNECTS_TO, from, to);
	}
}
