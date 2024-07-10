package hu.bme.mit.trainbenchmark.generator.refinery;

import hu.bme.mit.trainbenchmark.constants.ModelConstants;
import hu.bme.mit.trainbenchmark.constants.Position;
import hu.bme.mit.trainbenchmark.constants.Signal;
import hu.bme.mit.trainbenchmark.generator.ModelSerializer;
import tools.refinery.store.map.Version;
import tools.refinery.store.model.Interpretation;
import tools.refinery.store.model.Model;
import tools.refinery.store.model.ModelStore;
import tools.refinery.store.representation.AnySymbol;
import tools.refinery.store.representation.Symbol;
import tools.refinery.store.tuple.Tuple;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

public class RefinerySerializer extends ModelSerializer{

	protected ModelStore store = null;
	protected Model model = null;
	protected Map<Long, Version> versions;
	long commits = 0;

	Symbol<Boolean> trackElementSymbol;
	Symbol<Boolean> regionSymbol;
	Symbol<Boolean> routeSymbol;
	Symbol<Boolean> segmentSymbol;
	Symbol<Boolean> sensorSymbol;
	Symbol<Boolean> semaphoreSymbol;
	Symbol<Boolean> switchSymbol;
	Symbol<Boolean> switchPositionSymbol;

	Symbol<Boolean> connectsToSymbol;
	Symbol<Boolean> elementsSymbol;
	Symbol<Boolean> exitSymbol;
	Symbol<Boolean> entrySymbol;
	Symbol<Boolean> followsSymbol;
	Symbol<Boolean> requiresSymbol;
	Symbol<Boolean> monitoredBySymbol;
	Symbol<Boolean> semaphoresSymbol;
	Symbol<Boolean> sensorsSymbol;
	Symbol<Boolean> targetSymbol;

	Symbol<Boolean> activeSymbol;
	Symbol<Integer> lengthSymbol;
	Symbol<Signal> signalSymbol;
	Symbol<Position> currentPositionSymbol;
	Symbol<Position> positionSymbol;

	Interpretation<Boolean> trackElementInterpretation;
	Interpretation<Boolean> regionInterpretation;
	Interpretation<Boolean> routeInterpretation;
	Interpretation<Boolean> segmentInterpretation;
	Interpretation<Boolean> sensorInterpretation;
	Interpretation<Boolean> semaphoreInterpretation;
	Interpretation<Boolean> switchInterpretation;
	Interpretation<Boolean> switchPositionInterpretation;

	Interpretation<Boolean> connectsToInterpretation;
	Interpretation<Boolean> elementsInterpretation;
	Interpretation<Boolean> exitInterpretation;
	Interpretation<Boolean> entryInterpretation;
	Interpretation<Boolean> followsInterpretation;
	Interpretation<Boolean> requiresInterpretation;
	Interpretation<Boolean> monitoredByInterpretation;
	Interpretation<Boolean> semaphoresInterpretation;
	Interpretation<Boolean> sensorsInterpretation;
	Interpretation<Boolean> targetInterpretation;

	Interpretation<Boolean> activeInterpretation;
	Interpretation<Integer> lengthInterpretation;
	Interpretation<Signal> signalInterpretation;
	Interpretation<Position> currentPositionInterpretation;
	Interpretation<Position> positionInterpretation;
;
	public RefinerySerializer() {
		versions  = new HashMap<>();

		trackElementSymbol = Symbol.of(ModelConstants.TRACKELEMENT, 1, Boolean.class, false);
		regionSymbol = Symbol.of(ModelConstants.REGION, 1, Boolean.class, false);
		routeSymbol = Symbol.of(ModelConstants.ROUTE, 1, Boolean.class, false);
		segmentSymbol = Symbol.of(ModelConstants.SEGMENT, 1, Boolean.class, false);
		sensorSymbol = Symbol.of(ModelConstants.SENSOR, 1, Boolean.class, false);
		semaphoreSymbol = Symbol.of(ModelConstants.SEMAPHORE, 1, Boolean.class, false);
		switchSymbol = Symbol.of(ModelConstants.SWITCH, 1, Boolean.class, false);
		switchPositionSymbol = Symbol.of(ModelConstants.SWITCHPOSITION, 1, Boolean.class, false);

		connectsToSymbol = Symbol.of(ModelConstants.CONNECTS_TO, 2, Boolean.class, false);
		elementsSymbol = Symbol.of(ModelConstants.ELEMENTS, 2, Boolean.class, false);
		exitSymbol = Symbol.of(ModelConstants.EXIT, 2, Boolean.class, false);
		entrySymbol = Symbol.of(ModelConstants.ENTRY, 2, Boolean.class, false);
		followsSymbol = Symbol.of(ModelConstants.FOLLOWS, 2, Boolean.class, false);
		requiresSymbol = Symbol.of(ModelConstants.REQUIRES, 2, Boolean.class, false);
		monitoredBySymbol = Symbol.of(ModelConstants.MONITORED_BY, 2, Boolean.class, false);
		semaphoresSymbol = Symbol.of(ModelConstants.SEMAPHORES, 2, Boolean.class, false);
		sensorsSymbol = Symbol.of(ModelConstants.SENSORS, 2, Boolean.class, false);
		targetSymbol = Symbol.of(ModelConstants.TARGET, 2, Boolean.class, false);

		activeSymbol = Symbol.of(ModelConstants.ACTIVE, 1, Boolean.class, false);
		lengthSymbol = Symbol.of(ModelConstants.LENGTH, 1, Integer.class, 0);
		signalSymbol = Symbol.of(ModelConstants.SIGNAL, 1, Signal.class, null);
		currentPositionSymbol = Symbol.of(ModelConstants.CURRENTPOSITION, 1, Position.class, null);
		positionSymbol = Symbol.of(ModelConstants.POSITION, 1, Position.class, null);

	}

	@Override
	public String syntax() {
		return null;
	}

	@Override
	public void initModel() throws IOException {
		store = ModelStore.builder().symbols(
				trackElementSymbol,
				regionSymbol,
				routeSymbol,
				segmentSymbol,
				sensorSymbol,
				semaphoreSymbol,
				switchSymbol,
				switchPositionSymbol,
				connectsToSymbol,
				elementsSymbol,
				exitSymbol,
				entrySymbol,
				followsSymbol,
				requiresSymbol,
				monitoredBySymbol,
				semaphoresSymbol,
				sensorsSymbol,
				targetSymbol,
				activeSymbol,
				lengthSymbol,
				signalSymbol,
				currentPositionSymbol,
				positionSymbol).build();
		model = store.createEmptyModel();

		trackElementInterpretation = model.getInterpretation(trackElementSymbol);
		regionInterpretation = model.getInterpretation(regionSymbol);
		routeInterpretation = model.getInterpretation(routeSymbol);
		segmentInterpretation = model.getInterpretation(segmentSymbol);
		sensorInterpretation = model.getInterpretation(sensorSymbol);
		semaphoreInterpretation = model.getInterpretation(semaphoreSymbol);
		switchInterpretation = model.getInterpretation(switchSymbol);
		switchPositionInterpretation = model.getInterpretation(switchPositionSymbol);

		connectsToInterpretation = model.getInterpretation(connectsToSymbol);
		elementsInterpretation = model.getInterpretation(elementsSymbol);
		exitInterpretation = model.getInterpretation(exitSymbol);
		entryInterpretation = model.getInterpretation(entrySymbol);
		followsInterpretation = model.getInterpretation(followsSymbol);
		requiresInterpretation = model.getInterpretation(requiresSymbol);
		monitoredByInterpretation = model.getInterpretation(monitoredBySymbol);
		semaphoresInterpretation = model.getInterpretation(semaphoresSymbol);
		sensorsInterpretation = model.getInterpretation(sensorsSymbol);
		targetInterpretation = model.getInterpretation(targetSymbol);

		activeInterpretation = model.getInterpretation(activeSymbol);
		lengthInterpretation = model.getInterpretation(lengthSymbol);
		signalInterpretation = model.getInterpretation(signalSymbol);
		currentPositionInterpretation = model.getInterpretation(currentPositionSymbol);
		positionInterpretation = model.getInterpretation(positionSymbol);
	}


	@Override
	public Object createVertex(int id, String type, Map<String, ?> attributes,
			Map<String, Object> outgoingEdges, Map<String, Object> incomingEdges) throws IOException {
		var vertex = Tuple.of(id);

		// 1. add to type map
		switch (type) {
		case ModelConstants.TRACKELEMENT:
			trackElementInterpretation.put(vertex, true);
			break;
		case ModelConstants.REGION:
			regionInterpretation.put(vertex, true);
			break;
		case ModelConstants.ROUTE:
			routeInterpretation.put(vertex, true);
			break;
		case ModelConstants.SEGMENT:
			segmentInterpretation.put(vertex, true);
			break;
		case ModelConstants.SENSOR:
			sensorInterpretation.put(vertex, true);
			break;
		case ModelConstants.SEMAPHORE:
			semaphoreInterpretation.put(vertex, true);
			break;
		case ModelConstants.SWITCH:
			switchInterpretation.put(vertex, true);
			break;
		case ModelConstants.SWITCHPOSITION:
			switchPositionInterpretation.put(vertex, true);
			break;
		}

		// 2. set attributes
		for(Entry<String, ?> attribute : attributes.entrySet()) {
			setAttribute(attribute.getKey(), vertex, attribute.getValue());
		}
		// 3. outgoing edges
		for(Entry<String, ?> outgoing : outgoingEdges.entrySet()) {
			this.createEdge(outgoing.getKey(), vertex, outgoing.getValue());
		}
		// 4. incoming edges
		for(Entry<String, ?> incoming : incomingEdges.entrySet()) {
			this.createEdge(incoming.getKey(), incoming.getValue(), vertex);
		}
		// finish
		return vertex;
	}

	@Override
	public void createEdge(String label, Object from, Object to) throws IOException {
		setEdge(label, from, to, true);
	}

	@Override
	public void removeEdge(String label, Object from, Object to) throws IOException {
		setEdge(label, from, to, false);
	}

	protected void setEdge(String label, Object from, Object to, Boolean value) throws IOException {

		switch (label) {
		case ModelConstants.CONNECTS_TO:
			connectsToInterpretation.put(Tuple.of(((Tuple) from).get(0), ((Tuple) to).get(0)), value);
			break;
		case ModelConstants.ELEMENTS:
			elementsInterpretation.put(Tuple.of(((Tuple) from).get(0), ((Tuple) to).get(0)), value);
			break;
		case ModelConstants.EXIT:
			exitInterpretation.put(Tuple.of(((Tuple) from).get(0), ((Tuple) to).get(0)), value);
			break;
		case ModelConstants.ENTRY:
			entryInterpretation.put(Tuple.of(((Tuple) from).get(0), ((Tuple) to).get(0)), value);
			break;
		case ModelConstants.FOLLOWS:
			followsInterpretation.put(Tuple.of(((Tuple) from).get(0), ((Tuple) to).get(0)), value);
			break;
		case ModelConstants.REQUIRES:
			requiresInterpretation.put(Tuple.of(((Tuple) from).get(0), ((Tuple) to).get(0)), value);
			break;
		case ModelConstants.MONITORED_BY:
			monitoredByInterpretation.put(Tuple.of(((Tuple) from).get(0), ((Tuple) to).get(0)), value);
			break;
		case ModelConstants.SEMAPHORES:
			semaphoresInterpretation.put(Tuple.of(((Tuple) from).get(0), ((Tuple) to).get(0)), value);
			break;
		case ModelConstants.SENSORS:
			sensorsInterpretation.put(Tuple.of(((Tuple) from).get(0), ((Tuple) to).get(0)), value);
			break;
		case ModelConstants.TARGET:
			targetInterpretation.put(Tuple.of(((Tuple) from).get(0), ((Tuple) to).get(0)), value);
			break;
		}
	}

	@Override
	public void setAttribute(String label, Object object, Object value) {
		switch (label) {
		case ModelConstants.ACTIVE:
			activeInterpretation.put((Tuple) object, (Boolean) value);
			break;
		case ModelConstants.LENGTH:
			lengthInterpretation.put((Tuple) object, (Integer) value);
			break;
		case ModelConstants.SIGNAL:
			signalInterpretation.put((Tuple) object, (Signal) value);
			break;
		case ModelConstants.CURRENTPOSITION:
			currentPositionInterpretation.put((Tuple) object, (Position) value);
			break;
		case ModelConstants.POSITION:
			positionInterpretation.put((Tuple) object, (Position) value);
			break;
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

	@Override
	public void endTransaction() {

	}
}
