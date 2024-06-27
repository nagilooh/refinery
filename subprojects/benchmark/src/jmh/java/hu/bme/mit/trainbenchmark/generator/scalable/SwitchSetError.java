package hu.bme.mit.trainbenchmark.generator.scalable;

import java.io.IOException;

import hu.bme.mit.trainbenchmark.constants.ModelConstants;
import hu.bme.mit.trainbenchmark.constants.Position;
import hu.bme.mit.trainbenchmark.generator.ModelSerializer;

public class SwitchSetError extends GenerationError{
	Object object;
	Position newPosition;
	public SwitchSetError(Object object, Position newPosition) {
		super();
		this.object = object;
		this.newPosition = newPosition;
	}
	@Override
	void apply(ModelSerializer serializer) throws IOException {
		serializer.setAttribute(ModelConstants.POSITION, object, newPosition);
	}
}
