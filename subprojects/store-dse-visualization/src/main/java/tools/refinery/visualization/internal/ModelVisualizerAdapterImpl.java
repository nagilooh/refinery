/*
 * SPDX-FileCopyrightText: 2021-2026 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.visualization.internal;

import tools.refinery.logic.term.truthvalue.TruthValue;
import tools.refinery.store.dse.transition.DesignSpaceExplorationAdapter;
import tools.refinery.store.dse.transition.Transformation;
import tools.refinery.store.dse.transition.statespace.StateSpaceStore;
import tools.refinery.store.map.Version;
import tools.refinery.store.model.Interpretation;
import tools.refinery.store.model.Model;
import tools.refinery.store.representation.AnySymbol;
import tools.refinery.store.tuple.Tuple;
import tools.refinery.visualization.ModelVisualizerAdapter;
import tools.refinery.visualization.ModelVisualizerStoreAdapter;

import java.io.*;
import java.util.*;
import java.util.stream.Collectors;

public class ModelVisualizerAdapterImpl implements ModelVisualizerAdapter {

	private final Model model;
	private final ModelVisualizerStoreAdapterImpl storeAdapter;
	private final Map<AnySymbol, Interpretation<?>> allInterpretations;
	private final String outputPath;
	private final Set<FileFormat> formats;
	private final boolean renderDesignSpace;
	private final boolean renderStates;
	private final StateSpaceStore stateSpaceStore;
	private final List<Transformation> transformations;

	private static final Map<Object, String> truthValueToDot = Map.of(
			TruthValue.TRUE, "1",
			TruthValue.FALSE, "0",
			TruthValue.UNKNOWN, "½",
			TruthValue.ERROR, "E",
			true, "1",
			false, "0"
	);

	private record ActivationReference(Version fromVersion, int transformationIndex, int activationIndex) {
	}

	public ModelVisualizerAdapterImpl(Model model, ModelVisualizerStoreAdapterImpl storeAdapter) {
		this.model = model;
		this.storeAdapter = storeAdapter;
		this.outputPath = storeAdapter.getOutputPath();
		this.formats = storeAdapter.getFormats();
		if (formats.isEmpty()) {
			formats.add(FileFormat.SVG);
		}
		this.renderDesignSpace = storeAdapter.isRenderDesignSpace();
		this.renderStates = storeAdapter.isRenderStates();

		this.allInterpretations = new HashMap<>();
		for (var symbol : storeAdapter.getStore().getSymbols()) {
			var arity = symbol.arity();
			if (arity < 1 || arity > 2) {
				continue;
			}
			var interpretation = (Interpretation<?>) model.getInterpretation(symbol);
			allInterpretations.put(symbol, interpretation);
		}
		this.stateSpaceStore = storeAdapter.getStateSpaceStore();
		this.transformations = model.getAdapter(DesignSpaceExplorationAdapter.class).getTransformations();
	}

	@Override
	public Model getModel() {
		return this.model;
	}

	@Override
	public ModelVisualizerStoreAdapter getStoreAdapter() {
		return this.storeAdapter;
	}

	private String createDotForCurrentModelState() {

		var unaryTupleToInterpretationsMap = new HashMap<Tuple, LinkedHashSet<Interpretation<?>>>();

		var sb = new StringBuilder();

		sb.append("digraph model {\n");
		sb.append("""
				node [
				\tstyle="filled, rounded"
				\tshape=plain
				\tpencolor="#00000088"
				\tfontname="Helvetica"
				]
				""");
		sb.append("""
				edge [
				\tlabeldistance=3
				\tfontname="Helvetica"
				]
				""");

		for (var entry : allInterpretations.entrySet()) {
			var key = entry.getKey();
			var arity = key.arity();
			var cursor = entry.getValue().getAll();
			if (arity == 1) {
				while (cursor.move()) {
					unaryTupleToInterpretationsMap.computeIfAbsent(cursor.getKey(), k -> new LinkedHashSet<>())
							.add(entry.getValue());
				}
			} else if (arity == 2) {
				while (cursor.move()) {
					var tuple = cursor.getKey();
					for (var i = 0; i < tuple.getSize(); i++) {
						var id = tuple.get(i);
						unaryTupleToInterpretationsMap.computeIfAbsent(Tuple.of(id), k -> new LinkedHashSet<>());
					}
					sb.append(drawEdge(cursor.getKey(), key, entry.getValue()));
				}
			}
		}
		for (var entry : unaryTupleToInterpretationsMap.entrySet()) {
			sb.append(drawElement(entry));
		}
		sb.append("}");
		return sb.toString();
	}

	private StringBuilder drawElement(Map.Entry<Tuple, LinkedHashSet<Interpretation<?>>> entry) {
		var sb = new StringBuilder();

		var tableStyle =  " CELLSPACING=\"0\" BORDER=\"2\" CELLBORDER=\"0\" CELLPADDING=\"4\" STYLE=\"ROUNDED\"";

		var key = entry.getKey();
		var id = key.get(0);
		var mainLabel = String.valueOf(id);
		var interpretations = entry.getValue();
		var backgroundColor = toBackgroundColorString(averageColor(interpretations));

		sb.append(id);
		sb.append(" [\n");
		sb.append("\tfillcolor=\"").append(backgroundColor).append("\"\n");
		sb.append("\tlabel=");
		if (interpretations.isEmpty()) {
			sb.append("<<TABLE").append(tableStyle).append(">\n\t<TR><TD>").append(mainLabel).append("</TD></TR>");
		}
		else {
			sb.append("<<TABLE").append(tableStyle).append(">\n\t\t<TR><TD COLSPAN=\"3\" BORDER=\"2\" SIDES=\"B\">")
					.append(mainLabel).append("</TD></TR>\n");
			for (var interpretation : interpretations) {
				var rawValue = interpretation.get(key);

				if (rawValue == null || rawValue.equals(TruthValue.FALSE) || rawValue.equals(false)) {
					continue;
				}
				var color = "black";
				if (rawValue.equals(TruthValue.ERROR)) {
					color = "red";
				}
				var value = truthValueToDot.getOrDefault(rawValue, rawValue.toString());
				var symbol = interpretation.getSymbol();

				if (symbol.valueType() == String.class) {
					value = "\"" + value + "\"";
				}
				sb.append("\t\t<TR><TD><FONT COLOR=\"").append(color).append("\">")
						.append(interpretation.getSymbol().name())
						.append("</FONT></TD><TD><FONT COLOR=\"").append(color).append("\">")
						.append("=</FONT></TD><TD><FONT COLOR=\"").append(color).append("\">").append(value)
						.append("</FONT></TD></TR>\n");
			}
		}
		sb.append("\t\t</TABLE>>\n");
		sb.append("]\n");

		return sb;
	}

	private String drawEdge(Tuple edge, AnySymbol symbol, Interpretation<?> interpretation) {
		var value = interpretation.get(edge);

		if (value == null || value.equals(TruthValue.FALSE) || value.equals(false)) {
			return "";
		}

		var sb = new StringBuilder();
		var style = "solid";
		var color = "black";
		if (value.equals(TruthValue.UNKNOWN)) {
			style = "dotted";
		}
		else if (value.equals(TruthValue.ERROR)) {
			style = "dashed";
			color = "red";
		}

		var from = edge.get(0);
		var to = edge.get(1);
		var name = symbol.name();
		sb.append(from).append(" -> ").append(to)
				.append(" [\n\tstyle=").append(style)
				.append("\n\tcolor=").append(color)
				.append("\n\tfontcolor=").append(color)
				.append("\n\tlabel=\"").append(name)
				.append("\"]\n");
		return sb.toString();
	}

	private String toBackgroundColorString(Integer[] backgroundColor) {
		if (backgroundColor.length == 3)
			return String.format("#%02x%02x%02x", backgroundColor[0], backgroundColor[1], backgroundColor[2]);
		else if (backgroundColor.length == 4)
			return String.format("#%02x%02x%02x%02x", backgroundColor[0], backgroundColor[1], backgroundColor[2],
					backgroundColor[3]);
		return null;
	}

	private Integer[] typeColor(String name) {
		@SuppressWarnings("squid:S2245")
		var random = new Random(name.hashCode());
		return new Integer[] { random.nextInt(128) + 128, random.nextInt(128) + 128, random.nextInt(128) + 128 };
	}

	private Integer[] averageColor(Set<Interpretation<?>> interpretations) {
		if(interpretations.isEmpty()) {
			return new Integer[]{256, 256, 256};
		}
		// TODO: Only use interpretations where the value is not false (or unknown)
		var symbols = interpretations.stream()
				.map(i -> typeColor(i.getSymbol().name())).toArray(Integer[][]::new);



		return new Integer[] {
				Arrays.stream(symbols).map(i -> i[0]).collect(Collectors.averagingInt(Integer::intValue)).intValue(),
				Arrays.stream(symbols).map(i -> i[1]).collect(Collectors.averagingInt(Integer::intValue)).intValue(),
				Arrays.stream(symbols).map(i -> i[2]).collect(Collectors.averagingInt(Integer::intValue)).intValue()
		};
	}

	private String createDotForModelState(Version version) {
		var currentVersion = model.getState();
		model.restore(version);
		var graph = createDotForCurrentModelState();
		model.restore(currentVersion);
		return graph;
	}

	private boolean saveDot(String dot, String filePath) {
		File file = new File(filePath);
		file.getParentFile().mkdirs();

		try (FileWriter writer = new FileWriter(file)) {
			writer.write(dot);
		} catch (IOException e) {
			return false;
		}
		return true;
	}

	private boolean renderDot(String dot, String filePath) {
		return renderDot(dot, FileFormat.SVG, filePath);
	}

	private boolean renderDot(String dot, FileFormat format, String filePath) {
		try {
			Process process = new ProcessBuilder(storeAdapter.getDotBinaryPath(), "-T" + format.getFormat(),
					"-o", filePath).start();

			OutputStream osToProcess = process.getOutputStream();
			PrintWriter pwToProcess = new PrintWriter(osToProcess);
			pwToProcess.write(dot);
			pwToProcess.close();
		} catch (IOException e) {
			return false;
		}
		return true;
	}

	private StringBuilder buildDesignSpaceTransitionsDot(List<StateSpaceStore.StateTransition> transitions) {
		StringBuilder transitionsBuilder = new StringBuilder();
		var originalVersion = model.getState();
		Version restoredVersion = null;

		try {
			for (var transition : transitions) {
				var fromState = this.stateSpaceStore.getStateId(transition.from());
				var toState = this.stateSpaceStore.getStateId(transition.to());
				var visitResult = transition.visitResult();
				var ruleName = this.transformations.get(visitResult.transformation()).getDefinition().rule().getName();
				var activationReference = new ActivationReference(transition.from(), visitResult.transformation(),
						visitResult.activation());
				if (!transition.from().equals(restoredVersion)) {
					model.restore(transition.from());
					restoredVersion = transition.from();
				}
				var activationTuple = resolveActivationTuple(activationReference);
				var activationLabel = activationTuple.toString();
				var label = ruleName + ", " + activationLabel;
				transitionsBuilder.append(fromState).append(" -> ").append(toState)
						.append(" [label=\"").append(transition.id()).append(": ").append(label).append("\"]\n");
			}
		} finally {
			model.restore(originalVersion);
		}
		return transitionsBuilder;
	}

	private Tuple resolveActivationTuple(ActivationReference activationReference) {
		int transformationIndex = activationReference.transformationIndex();
		int activationIndex = activationReference.activationIndex();

		Tuple activation;
		if (transformationIndex < 0 || transformationIndex >= this.transformations.size()) {
			throw new IllegalStateException("Transformation index %d is out of bounds for state %s"
					.formatted(transformationIndex, activationReference.fromVersion()));
		}
		var transformation = this.transformations.get(transformationIndex);
		int activationCount = transformation.getAllActivationsAsResultSet().size();
		if (activationIndex < 0 || activationIndex >= activationCount) {
			throw new IllegalStateException("Activation index %d is out of bounds (size=%d) for transformation %d in state %s"
					.formatted(activationIndex, activationCount, transformationIndex, activationReference.fromVersion()));
		}
		activation = transformation.getActivation(activationIndex);

		return activation;
	}

	private String buildDesignSpaceDot(boolean renderTransitionsToAlreadyVisitedStates) {
		StringBuilder designSpaceBuilder = new StringBuilder();
		designSpaceBuilder.append("digraph designSpace {\n");
		designSpaceBuilder.append("""
				nodesep=0
				ranksep=5
				node[
				\tstyle=filled
				\tfillcolor=white
				]
				""");

		for (var state : this.stateSpaceStore.getStates()) {

			designSpaceBuilder.append(state.id()).append(" [label = \"").append(state.id()).append(" (");
			designSpaceBuilder.append(state.objectiveValue());
			designSpaceBuilder.append(")\"\n").append("URL=\"./").append(state.id()).append(".svg\"");
			if (state.isSolution()) {
				designSpaceBuilder.append(" peripheries = 2");
			}
			designSpaceBuilder.append("]\n");
		}

		designSpaceBuilder.append(buildDesignSpaceTransitionsDot(this.stateSpaceStore.getTransitions()));


		if (renderTransitionsToAlreadyVisitedStates) {
			designSpaceBuilder.append(buildDesignSpaceTransitionsDot(this.stateSpaceStore.getTransitionsToAlreadyVisited()));
		}

		designSpaceBuilder.append("}");
		return designSpaceBuilder.toString();
	}

	@Override
	public void visualize(StateSpaceStore stateSpaceStore, boolean renderTransitionsToAlreadyVisitedStates) {
		File filePath = new File(this.outputPath);
		filePath.mkdirs();

		if (renderStates) {
			for (var state : this.stateSpaceStore.getStates()) {
				var stateId = state.id();
				var stateVersion = state.version();
				var stateDot = createDotForModelState(stateVersion);
				for (var format : this.formats) {
					if (format == FileFormat.DOT) {
						saveDot(stateDot, this.outputPath + "/" + stateId + ".dot");
					}
					else {
						renderDot(stateDot, format, this.outputPath + "/" + stateId + "." + format.getFormat());
					}
				}
			}
		}

		if (renderDesignSpace) {
			var designSpaceDot = buildDesignSpaceDot(renderTransitionsToAlreadyVisitedStates);
			for (var format : this.formats) {
				if (format == FileFormat.DOT) {
					saveDot(designSpaceDot, this.outputPath + "/designSpace.dot");
				}
				else {
					renderDot(designSpaceDot, format, this.outputPath + "/designSpace." + format.getFormat());
				}
			}
		}
	}
}
