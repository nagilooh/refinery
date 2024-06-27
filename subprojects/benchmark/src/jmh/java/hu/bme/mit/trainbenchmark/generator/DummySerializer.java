package hu.bme.mit.trainbenchmark.generator;

import java.io.IOException;
import java.util.Map;
import java.util.Map.Entry;

public class DummySerializer extends ModelSerializer{

	@Override
	public String syntax() {
		// TODO Auto-generated method stub
		return null;
	}

	int vertices = 0;
	public int getVertices() {
		return vertices;
	}

	@Override
	public void initModel() throws IOException {
	}

	@Override
	public Object createVertex(int id, String type, Map<String, ? extends Object> attributes,
			Map<String, Object> outgoingEdges, Map<String, Object> incomingEdges) throws IOException {
		vertices ++;
		vertices += attributes.size();
		vertices += outgoingEdges.size();
		vertices += incomingEdges.size();
		return id;
	}

	@Override
	public void createEdge(String label, Object from, Object to) throws IOException {
		vertices++;
	}

	@Override
	public void removeEdge(String label, Object from, Object to) throws IOException {
		vertices--;
	}

	@Override
	public void setAttribute(String label, Object object, Object value) throws IOException {

	}
	long commits = 0;
	@Override
	public long commit() {
		return commits++;
	}
	public long getCommits() {
		return commits;
	}

	@Override
	public void restore(long version) {
		// TODO Auto-generated method stub

	}
}
