package hu.bme.mit.trainbenchmark.generator;

import java.io.IOException;
import java.util.Collections;
import java.util.Map;

public abstract class ModelSerializer {

	protected int id = 1;

	public ModelSerializer() {
	}

	public abstract String syntax();

	public abstract void initModel() throws IOException;

	// the createVertex() methods with fewer arguments are final

	public final Object createVertex(final String type) throws IOException {
		return createVertex(type, Collections.<String, Object> emptyMap(), Collections.<String, Object> emptyMap(),
				Collections.<String, Object> emptyMap());
	}

	public final Object createVertex(final String type, final Map<String, ? extends Object> attributes) throws IOException {
		return createVertex(type, attributes, Collections.<String, Object> emptyMap(), Collections.<String, Object> emptyMap());
	}

	public final Object createVertex(final String type, final Map<String, ? extends Object> attributes,
			final Map<String, Object> outgoingEdges) throws IOException {
		return createVertex(type, attributes, outgoingEdges, Collections.<String, Object> emptyMap());
	}

	public final Object createVertex(final String type, final Map<String, ? extends Object> attributes,
			final Map<String, Object> outgoingEdges, final Map<String, Object> incomingEdges) throws IOException {
		final Object vertex = createVertex(id, type, attributes, outgoingEdges, incomingEdges);
		id++;
		return vertex;
	}

	public abstract Object createVertex(final int id, final String type, final Map<String, ? extends Object> attributes,
			final Map<String, Object> outgoingEdges, final Map<String, Object> incomingEdges) throws IOException;

	public abstract void createEdge(String label, Object from, Object to) throws IOException;

	public abstract void removeEdge(String label, Object from, Object to) throws IOException;

	public abstract void setAttribute(String label, Object object, Object value) throws IOException;

	public abstract long commit();
	public abstract void restore(long version);

	public abstract void endTransaction();
}
