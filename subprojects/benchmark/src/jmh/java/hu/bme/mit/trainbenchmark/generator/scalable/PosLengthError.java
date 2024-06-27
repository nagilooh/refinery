package hu.bme.mit.trainbenchmark.generator.scalable;

import java.io.IOException;

import hu.bme.mit.trainbenchmark.constants.ModelConstants;
import hu.bme.mit.trainbenchmark.generator.ModelSerializer;

public class PosLengthError extends GenerationError {
	Object source;
	int newLength;

	public PosLengthError(Object source, int newLength) {
		super();
		this.source = source;
		this.newLength = newLength;
	}

	@Override
	void apply(ModelSerializer serializer) throws IOException {
		serializer.setAttribute(ModelConstants.LENGTH, source, newLength);
	}
}
