package hu.bme.mit.trainbenchmark.generator.scalable;

import static hu.bme.mit.trainbenchmark.constants.ModelConstants.ELEMENTS;
import static hu.bme.mit.trainbenchmark.constants.ModelConstants.LENGTH;
import static hu.bme.mit.trainbenchmark.constants.ModelConstants.MONITORED_BY;
import static hu.bme.mit.trainbenchmark.constants.ModelConstants.SEGMENT;

import java.io.IOException;
import java.util.Map;

import hu.bme.mit.trainbenchmark.generator.ModelSerializer;

public class ConnectedSegmentsError extends GenerationError {
	int segmentLength;
	Object region;
	Object sensor;

	public ConnectedSegmentsError(int segmentLength, Object region, Object sensor) {
		super();
		this.segmentLength = segmentLength;
		this.region = region;
		this.sensor = sensor;
	}

	@Override
	void apply(ModelSerializer serializer) throws IOException {
		final Map<String, Object> segmentAttributes = Map.of(LENGTH, segmentLength);
		final Object segment = serializer.createVertex(SEGMENT, segmentAttributes);

		// (region)-[:elements]->(segment)
		serializer.createEdge(ELEMENTS, region, segment);

		// (segment)-[:monitoredBy]->(sensor) monitoredBy n:m edge
		serializer.createEdge(MONITORED_BY, segment, sensor);
	}

}
