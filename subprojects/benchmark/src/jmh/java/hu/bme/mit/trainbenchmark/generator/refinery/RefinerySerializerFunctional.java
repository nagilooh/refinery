package hu.bme.mit.trainbenchmark.generator.refinery;

import hu.bme.mit.trainbenchmark.constants.ModelConstants;
import hu.bme.mit.trainbenchmark.constants.Position;
import hu.bme.mit.trainbenchmark.constants.Signal;
import hu.bme.mit.trainbenchmark.generator.ModelSerializer;
import hu.bme.mit.trainbenchmark.generator.scalable.ScalableModelGenerator;
import tools.refinery.store.map.Version;
import tools.refinery.store.model.Interpretation;
import tools.refinery.store.model.Model;
import tools.refinery.store.model.ModelStore;
import tools.refinery.store.representation.Symbol;
import tools.refinery.store.tuple.Tuple;
import tools.refinery.store.tuple.Tuple1;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

public class RefinerySerializerFunctional extends ModelSerializer{

	protected ModelStore store = null;
	protected Model model = null;
	protected Map<Long, Version> versions;
	long commits = 0;

	// Functional
	Symbol<List> exitSymbol;
	Symbol<List> entrySymbol;
	Symbol<List> targetSymbol;
	Symbol<List> connectsToInverseSymbol; // only if ScalableModelGenerator.inverseConnectsTo
	Symbol<List> elementsSymbol;
	Symbol<List> sensorsSymbol;
	Symbol<List> followsSymbol;
	Symbol<List> semaphoresSymbol;

	// Relational
	Symbol<Boolean> trackElementSymbol;
	Symbol<Boolean> regionSymbol;
	Symbol<Boolean> routeSymbol;
	Symbol<Boolean> segmentSymbol;
	Symbol<Boolean> sensorSymbol;
	Symbol<Boolean> semaphoreSymbol;
	Symbol<Boolean> switchSymbol;
	Symbol<Boolean> switchPositionSymbol;
	Symbol<Boolean> connectsToSymbol; // only if !ScalableModelGenerator.inverseConnectsTo

	Symbol<Boolean> requiresSymbol;
	Symbol<Boolean> monitoredBySymbol;

	Symbol<Boolean> activeSymbol;
	Symbol<Integer> lengthSymbol;
	Symbol<Signal> signalSymbol;
	Symbol<Position> currentPositionSymbol;
	Symbol<Position> positionSymbol;

	// Functional
	Interpretation<List> exitInterpretation;
	Interpretation<List> entryInterpretation;
	Interpretation<List> targetInterpretation;
	Interpretation<List> connectsToInverseInterpretation; // only if ScalableModelGenerator.inverseConnectsTo
	Interpretation<List> elementsInterpretation;
	Interpretation<List> sensorsInterpretation;
	Interpretation<List> followsInterpretation;
	Interpretation<List> semaphoresInterpretation;

	// Relational
	Interpretation<Boolean> trackElementInterpretation;
	Interpretation<Boolean> regionInterpretation;
	Interpretation<Boolean> routeInterpretation;
	Interpretation<Boolean> segmentInterpretation;
	Interpretation<Boolean> sensorInterpretation;
	Interpretation<Boolean> semaphoreInterpretation;
	Interpretation<Boolean> switchInterpretation;
	Interpretation<Boolean> switchPositionInterpretation;
	Interpretation<Boolean> connectsToInterpretation; // only if !ScalableModelGenerator.inverseConnectsTo

	Interpretation<Boolean> requiresInterpretation;
	Interpretation<Boolean> monitoredByInterpretation;

	Interpretation<Boolean> activeInterpretation;
	Interpretation<Integer> lengthInterpretation;
	Interpretation<Signal> signalInterpretation;
	Interpretation<Position> currentPositionInterpretation;
	Interpretation<Position> positionInterpretation;
;
	public RefinerySerializerFunctional() {
		versions  = new HashMap<>();

		// Functional
		exitSymbol = Symbol.of(ModelConstants.EXIT, 1, List.class, new ArrayList<Tuple1>());
		entrySymbol = Symbol.of(ModelConstants.ENTRY, 1, List.class, new ArrayList<Tuple1>());
		targetSymbol = Symbol.of(ModelConstants.TARGET, 1, List.class, new ArrayList<Tuple1>());
		connectsToInverseSymbol = Symbol.of(ModelConstants.CONNECTS_TO, 1, List.class, new ArrayList<Tuple1>());
		elementsSymbol = Symbol.of(ModelConstants.ELEMENTS, 1, List.class, new ArrayList<Tuple1>());
		sensorsSymbol = Symbol.of(ModelConstants.SENSORS, 1, List.class, new ArrayList<Tuple1>());
		followsSymbol = Symbol.of(ModelConstants.FOLLOWS, 1, List.class, new ArrayList<Tuple1>());
		semaphoresSymbol = Symbol.of(ModelConstants.SEMAPHORES, 1, List.class, new ArrayList<Tuple1>());

		// Relational
		trackElementSymbol = Symbol.of(ModelConstants.TRACKELEMENT, 1, Boolean.class, false);
		regionSymbol = Symbol.of(ModelConstants.REGION, 1, Boolean.class, false);
		routeSymbol = Symbol.of(ModelConstants.ROUTE, 1, Boolean.class, false);
		segmentSymbol = Symbol.of(ModelConstants.SEGMENT, 1, Boolean.class, false);
		sensorSymbol = Symbol.of(ModelConstants.SENSOR, 1, Boolean.class, false);
		semaphoreSymbol = Symbol.of(ModelConstants.SEMAPHORE, 1, Boolean.class, false);
		switchSymbol = Symbol.of(ModelConstants.SWITCH, 1, Boolean.class, false);
		switchPositionSymbol = Symbol.of(ModelConstants.SWITCHPOSITION, 1, Boolean.class, false);

		requiresSymbol = Symbol.of(ModelConstants.REQUIRES, 2, Boolean.class, false);
		monitoredBySymbol = Symbol.of(ModelConstants.MONITORED_BY, 2, Boolean.class, false);
		connectsToSymbol = Symbol.of(ModelConstants.CONNECTS_TO, 2, Boolean.class, false);

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
				connectsToInverseSymbol,
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

		connectsToInverseInterpretation = model.getInterpretation(connectsToInverseSymbol);
		elementsInterpretation = model.getInterpretation(elementsSymbol);
		exitInterpretation = model.getInterpretation(exitSymbol);
		entryInterpretation = model.getInterpretation(entrySymbol);
		followsInterpretation = model.getInterpretation(followsSymbol);
		requiresInterpretation = model.getInterpretation(requiresSymbol);
		monitoredByInterpretation = model.getInterpretation(monitoredBySymbol);
		semaphoresInterpretation = model.getInterpretation(semaphoresSymbol);
		sensorsInterpretation = model.getInterpretation(sensorsSymbol);
		targetInterpretation = model.getInterpretation(targetSymbol);
		connectsToInterpretation = model.getInterpretation(connectsToSymbol);

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
		// TODO check if it needs inverting
		switch (label) {
		case ModelConstants.EXIT:
			exitInterpretation.get((Tuple1) from).add(to);
			break;
		case ModelConstants.ENTRY:
			entryInterpretation.get((Tuple1) from).add(to);
			break;
		case ModelConstants.TARGET:
			targetInterpretation.get((Tuple1) from).add(to);
			break;
		case ModelConstants.ELEMENTS:
			elementsInterpretation.get((Tuple1) from).add(to);
			break;
		case ModelConstants.SENSORS:
			sensorsInterpretation.get((Tuple1) from).add(to);
			break;
		case ModelConstants.FOLLOWS:
			followsInterpretation.get((Tuple1) from).add(to);
			break;
		case ModelConstants.SEMAPHORES:
			semaphoresInterpretation.get((Tuple1) from).add(to);
			break;
		case ModelConstants.CONNECTS_TO:
			if (ScalableModelGenerator.inverseConnectsTo) {
				var value = connectsToInverseInterpretation.get((Tuple1) to);
				value.add(from);
//				connectsToInverseInterpretation.put((Tuple1) to, value); // Do we need this?
			}
			else {
				connectsToInterpretation.put(Tuple.of(((Tuple) from).get(0), ((Tuple) to).get(0)), true);
			}
			break;
		case ModelConstants.REQUIRES:
			requiresInterpretation.put(Tuple.of(((Tuple) from).get(0), ((Tuple) to).get(0)), true);
			break;
		case ModelConstants.MONITORED_BY:
			monitoredByInterpretation.put(Tuple.of(((Tuple) from).get(0), ((Tuple) to).get(0)), true);
			break;
		}
	}

	@Override
	public void removeEdge(String label, Object from, Object to) throws IOException {
		// TODO check if it needs inverting
		switch (label) {
		case ModelConstants.EXIT:
			exitInterpretation.get((Tuple1) from).remove(to);
			break;
		case ModelConstants.ENTRY:
			entryInterpretation.get((Tuple1) from).remove(to);
			break;
		case ModelConstants.TARGET:
			targetInterpretation.get((Tuple1) from).remove(to);
			break;
		case ModelConstants.ELEMENTS:
			elementsInterpretation.get((Tuple1) from).remove(to);
			break;
		case ModelConstants.SENSORS:
			sensorsInterpretation.get((Tuple1) from).remove(to);
			break;
		case ModelConstants.FOLLOWS:
			followsInterpretation.get((Tuple1) from).remove(to);
			break;
		case ModelConstants.SEMAPHORES:
			semaphoresInterpretation.get((Tuple1) from).remove(to);
			break;
		case ModelConstants.CONNECTS_TO:
			if (ScalableModelGenerator.inverseConnectsTo) {
				connectsToInverseInterpretation.get((Tuple1) to).remove(to);
//				connectsToInverseInterpretation.put((Tuple1) to, value); // Do we need this?
			}
			else {
				connectsToInterpretation.put(Tuple.of(((Tuple) from).get(0), ((Tuple) to).get(0)), false); // check
				// if we
				// need to switch to and from
			}
			break;
		case ModelConstants.REQUIRES:
			requiresInterpretation.put(Tuple.of(((Tuple) from).get(0), ((Tuple) to).get(0)), false);
			break;
		case ModelConstants.MONITORED_BY:
			monitoredByInterpretation.put(Tuple.of(((Tuple) from).get(0), ((Tuple) to).get(0)), false);
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
