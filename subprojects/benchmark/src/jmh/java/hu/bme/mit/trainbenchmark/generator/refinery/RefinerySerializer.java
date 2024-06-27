package hu.bme.mit.trainbenchmark.generator.refinery;

import java.io.IOException;
import java.util.*;
import java.util.Map.Entry;

import hu.bme.mit.trainbenchmark.constants.ModelConstants;
import hu.bme.mit.trainbenchmark.generator.ModelSerializer;
import tools.refinery.store.map.Version;
import tools.refinery.store.model.AnyInterpretation;
import tools.refinery.store.model.Interpretation;
import tools.refinery.store.model.Model;
import tools.refinery.store.model.ModelStore;
import tools.refinery.store.representation.AnySymbol;
import tools.refinery.store.representation.Symbol;
import tools.refinery.store.tuple.Tuple;

public class RefinerySerializer extends ModelSerializer{

	Map<String, AnySymbol> symbols;
	Map<String, AnyInterpretation> interpretations;
	protected ModelStore store = null;
	protected Model model = null;
	protected Map<Long, Version> versions;
	long commits = 0;

	public RefinerySerializer() {
		symbols = new HashMap<>();
		interpretations = new HashMap<>();
		versions  = new HashMap<>();
		String[] types = new String[] {
				ModelConstants.TRACKELEMENT,
				ModelConstants.REGION,
				ModelConstants.ROUTE,
				ModelConstants.SEGMENT,
				ModelConstants.SENSOR,
				ModelConstants.SEMAPHORE,
				ModelConstants.SWITCH,
				ModelConstants.SWITCHPOSITION
		};
		for(String type : types) {
			initType(symbols, type);
		}

		String[] references = new String[] {
				ModelConstants.CONNECTS_TO,
				ModelConstants.ELEMENTS,
				ModelConstants.EXIT,
				ModelConstants.ENTRY,
				ModelConstants.FOLLOWS,
				ModelConstants.REQUIRES,
				ModelConstants.MONITORED_BY,
				ModelConstants.SEMAPHORES,
				ModelConstants.SENSORS,
				ModelConstants.TARGET
		};
		for(String reference : references) {
			initReference(symbols, reference);
		}


		symbols.put(ModelConstants.ACTIVE, Symbol.of(ModelConstants.ACTIVE, 1, Boolean.class, false));
		symbols.put(ModelConstants.LENGTH, Symbol.of(ModelConstants.LENGTH, 1, Integer.class, 0));
		symbols.put(ModelConstants.SIGNAL, Symbol.of(ModelConstants.SIGNAL, 1, Enum.class, null));
		symbols.put(ModelConstants.CURRENTPOSITION, Symbol.of(ModelConstants.CURRENTPOSITION, 1, Enum.class, null));
		symbols.put(ModelConstants.POSITION, Symbol.of(ModelConstants.POSITION, 1, Enum.class, null));

	}

	@Override
	public String syntax() {
		return null;
	}

	@Override
	public void initModel() throws IOException {
		store = ModelStore.builder().symbols(symbols.values()).build();
		model = store.createEmptyModel();
		for(Entry<String, AnySymbol> entry : symbols.entrySet()) {
			String type = entry.getKey();
			AnySymbol symbol = entry.getValue();
			interpretations.put(type, model.getInterpretation(symbol));
		}
	}


	protected void initType(Map<String, AnySymbol> symbols, String name) {
		symbols.put(name, Symbol.of(name, 1, Boolean.class, false));
	}
	protected void initReference(Map<String, AnySymbol> symbols, String name) {
		symbols.put(name, Symbol.of(name, 2, Boolean.class, false));
	}


	@Override
	public Object createVertex(int id, String type, Map<String, ?> attributes,
			Map<String, Object> outgoingEdges, Map<String, Object> incomingEdges) throws IOException {
		var vertex = Tuple.of(id);

		// 1. add to type map
		var vertexInterpretation = (Interpretation<Boolean>) interpretations.get(type);
		vertexInterpretation.put(vertex, true);
		// 2. set attributes
		for(Entry<String, ? extends Object> attribute : attributes.entrySet()) {
			setAttribute(attribute.getKey(), vertex, attribute.getValue());
		}
		// 3. outgoing edges
		for(Entry<String, ? extends Object> outgoing : outgoingEdges.entrySet()) {
			this.createEdge(outgoing.getKey(), vertex, outgoing.getValue());
		}
		// 4. incoming edges
		for(Entry<String, ? extends Object> incoming : incomingEdges.entrySet()) {
			this.createEdge(incoming.getKey(), incoming.getValue(), vertex);
		}
		// finish
		return vertex;
	}

	@Override
	public void createEdge(String label, Object from, Object to) throws IOException {
		var edgeInterpretation = (Interpretation<Boolean>) interpretations.get(label);
		edgeInterpretation.put(Tuple.of(((Tuple) from).get(0), ((Tuple) to).get(0)), true);
	}

	@Override
	public void removeEdge(String label, Object from, Object to) throws IOException {
		var edgeInterpretation = (Interpretation<Boolean>) interpretations.get(label);
		edgeInterpretation.put(Tuple.of(((Tuple) from).get(0), ((Tuple) to).get(0)), false);
	}

	@Override
	public void setAttribute(String label, Object object, Object value) {
		if (value instanceof Integer) {
			var attributeInterpretation = (Interpretation<Integer>) interpretations.get(label);
			attributeInterpretation.put((Tuple) object, (Integer) value);
		}
		else if (value instanceof Boolean) {
			var attributeInterpretation = (Interpretation<Boolean>) interpretations.get(label);
			attributeInterpretation.put((Tuple) object, (Boolean) value);
		}
		else if (value instanceof Enum) {
			var attributeInterpretation = (Interpretation<Enum>) interpretations.get(label);
			attributeInterpretation.put((Tuple) object, (Enum) value);
		}

	}

	@Override
	public long commit() {
		versions.put(commits, this.model.commit());
		return commits++;

	}
	@Override
	public void restore(long version) {
		this.model.restore(versions.get(version));
	}
}
