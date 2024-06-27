package hu.bme.mit.trainbenchmark.generator.scalable;

import java.io.IOException;

import hu.bme.mit.trainbenchmark.constants.ModelConstants;
import hu.bme.mit.trainbenchmark.generator.ModelSerializer;

public class SemaphoreNeighborError extends GenerationError {
	Object source;
	Object entry;
	
	public SemaphoreNeighborError(Object source, Object entry) {
		super();
		this.source = source;
		this.entry = entry;
	}

	@Override
	void apply(ModelSerializer serializer) throws IOException {
		serializer.removeEdge(ModelConstants.ENTRY, source, entry);
	}
}
