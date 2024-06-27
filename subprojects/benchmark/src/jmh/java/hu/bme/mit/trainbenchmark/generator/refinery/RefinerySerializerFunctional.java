package hu.bme.mit.trainbenchmark.generator.refinery;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import hu.bme.mit.trainbenchmark.constants.ModelConstants;
import hu.bme.mit.trainbenchmark.generator.scalable.ScalableModelGenerator;
import tools.refinery.store.model.Interpretation;
import tools.refinery.store.representation.AnySymbol;
import tools.refinery.store.representation.Symbol;
import tools.refinery.store.tuple.Tuple;
import tools.refinery.store.tuple.Tuple1;

public class RefinerySerializerFunctional extends RefinerySerializer{
	protected boolean isFunctional(String label) {
		return
				Objects.equals(label, ModelConstants.EXIT) ||
				Objects.equals(label, ModelConstants.ENTRY) ||
				Objects.equals(label, ModelConstants.TARGET) ||
				(Objects.equals(label, ModelConstants.CONNECTS_TO) && ScalableModelGenerator.inverseConnectsTo) ||
				// Objects.equals(label, ModelConstants.MONITORED_BY) || // almost
				Objects.equals(label, ModelConstants.ELEMENTS) ||
				Objects.equals(label, ModelConstants.SENSORS) ||
				Objects.equals(label, ModelConstants.FOLLOWS) ||
				Objects.equals(label, ModelConstants.SEMAPHORES);
	}
	protected boolean inverting(String label) {
		return Objects.equals(label, ModelConstants.ELEMENTS) ||
				Objects.equals(label, ModelConstants.SENSORS) ||
				Objects.equals(label, ModelConstants.FOLLOWS) ||
				Objects.equals(label, ModelConstants.SEMAPHORES);
	}



	@Override
	protected void initReference(Map<String, AnySymbol> symbols, String name) {
		if(isFunctional(name)) {
			initFuncReference(symbols, name);
		} else {
			initRelReference(symbols, name);
		}
	}

	private void initRelReference(Map<String, AnySymbol> symbols, String name) {
		symbols.put(name, Symbol.of(name, 2, Boolean.class, false));
	}
	private void initFuncReference(Map<String, AnySymbol> symbols, String name) {
		symbols.put(name, Symbol.of(name, 1, List.class, new ArrayList<Tuple1>()));
	}

	@Override
	public void createEdge(String label, Object from, Object to) throws IOException {
		if(isFunctional(label)) {
			var edgeInterpretation = (Interpretation<List>) interpretations.get(label);
			if(inverting(label)) {
				var value = edgeInterpretation.get((Tuple1) to);
				value.add(from);
				edgeInterpretation.put((Tuple1) to, value);
			} else {
				var value = edgeInterpretation.get((Tuple1) from);
				value.add(to);
				edgeInterpretation.put((Tuple1) from, value);
			}
		} else {
			var edgeInterpretation = (Interpretation<Boolean>) interpretations.get(label);
			edgeInterpretation.put(Tuple.of(((Tuple1) from).get(0), ((Tuple1) to).get(0)), true);
		}
	}

	@Override
	public void removeEdge(String label, Object from, Object to) throws IOException {
		if(isFunctional(label)) {
			var edgeInterpretation = (Interpretation<Tuple1>) interpretations.get(label);
			if(inverting(label)) {
				edgeInterpretation.put((Tuple1) to, null);
			} else {
				edgeInterpretation.put((Tuple1) from, null);
			}
		} else {
			var edgeInterpretation = (Interpretation<Boolean>) interpretations.get(label);
			edgeInterpretation.put(Tuple.of(((Tuple1) from).get(0), ((Tuple1) to).get(0)), false);
		}
	}
}
