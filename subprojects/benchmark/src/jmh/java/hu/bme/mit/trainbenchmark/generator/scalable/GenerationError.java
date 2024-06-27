package hu.bme.mit.trainbenchmark.generator.scalable;

import java.io.IOException;

import hu.bme.mit.trainbenchmark.generator.ModelSerializer;

public abstract class GenerationError {
	abstract void apply(ModelSerializer serializer) throws IOException;
}
