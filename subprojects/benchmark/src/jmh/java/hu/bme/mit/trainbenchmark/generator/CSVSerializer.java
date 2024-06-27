package hu.bme.mit.trainbenchmark.generator;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

public class CSVSerializer extends ModelSerializer {

	public CSVSerializer() {
		super();
	}

	@Override
	public String syntax() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void initModel() throws IOException {
		// TODO Auto-generated method stub

	}

	protected final String delim = " ";

	List<Object[]> content = new ArrayList<>();

	protected void saveTuple(String symbol, Object... objects) {
		content.add(new Object[] {symbol,objects});
		triple++;
	}
	public long triple = 0;

	@Override
	public Object createVertex(int id, String type, Map<String, ? extends Object> attributes,
			Map<String, Object> outgoingEdges, Map<String, Object> incomingEdges) throws IOException {
		Integer object = id;
		saveTuple(type, id);
		for(Entry<String, ? extends Object> entry: attributes.entrySet()) {
			saveTuple(entry.getKey(), id, entry.getValue());
		}
		for(Entry<String, ? extends Object> entry: outgoingEdges.entrySet()) {
			saveTuple(entry.getKey(), id, entry.getValue());
		}
		for(Entry<String, ? extends Object> entry: incomingEdges.entrySet()) {
			saveTuple(entry.getKey(), entry.getValue(), id);
		}
		return object;
	}

	@Override
	public void createEdge(String label, Object from, Object to) throws IOException {
		saveTuple(label, from, to);
	}

	@Override
	public void removeEdge(String label, Object from, Object to) throws IOException {
		throw new UnsupportedOperationException();
	}
	@Override
	public void setAttribute(String label, Object object, Object value) throws IOException {
		saveTuple(label, object, value);
	}

	@Override
	public long commit() {
		// TODO Auto-generated method stub
		return 0;
	}
	@Override
	public void restore(long version) {
		// TODO Auto-generated method stub

	}
}
