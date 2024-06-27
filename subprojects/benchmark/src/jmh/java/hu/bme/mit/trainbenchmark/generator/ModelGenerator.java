package hu.bme.mit.trainbenchmark.generator;

import java.io.FileNotFoundException;
import java.io.IOException;

public abstract class ModelGenerator {

	protected final ModelSerializer serializer;

	public ModelGenerator(final ModelSerializer serializer) {
		this.serializer = serializer;
	}

	public void generateModel() throws Exception {
		serializer.initModel();
		constructModel();
	}

	protected abstract void constructModel() throws FileNotFoundException, IOException;

}
