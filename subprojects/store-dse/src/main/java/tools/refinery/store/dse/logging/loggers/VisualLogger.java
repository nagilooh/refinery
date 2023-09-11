/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.dse.logging.loggers;

import org.eclipse.collections.api.factory.primitive.ObjectIntMaps;
import org.eclipse.collections.api.map.primitive.MutableObjectIntMap;
import tools.refinery.store.dse.logging.Logger;
import tools.refinery.store.dse.transition.VersionWithObjectiveValue;
import tools.refinery.store.map.Version;
import tools.refinery.store.model.Interpretation;
import tools.refinery.store.model.Model;
import tools.refinery.store.model.ModelStore;
import tools.refinery.store.representation.AnySymbol;
import tools.refinery.store.representation.TruthValue;
import tools.refinery.store.tuple.Tuple;

import java.io.*;
import java.util.*;
import java.util.stream.Collectors;

public class VisualLogger implements Logger {

	private String outputPath;
	private boolean isSaveDesignSpace = false;
	private boolean isSaveStates = false;
	private final Set<FileFormat> formats = new LinkedHashSet<>();

	private final Map<Version, Integer> states = new HashMap<>();
	private final MutableObjectIntMap<Version> depths = ObjectIntMaps.mutable.empty();
	private int transitionCounter = 0;
	private Integer numberOfStates = 0;
	private final StringBuilder designSpaceBuilder = new StringBuilder();

	private Model model;
	private final Map<AnySymbol, Interpretation<?>> allInterpretations = new HashMap<>();

	private static final Map<Object, String> truthValueToDot = Map.of(
			TruthValue.TRUE, "1",
			TruthValue.FALSE, "0",
			TruthValue.UNKNOWN, "U",
			TruthValue.ERROR, "E",
			true, "1",
			false, "0"
	);

	private String executablePath;

	public VisualLogger withOutputPath(String outputPath) {
		this.outputPath = outputPath;
		return this;
	}

	public VisualLogger withFormat(FileFormat format) {
		this.formats.add(format);
		return this;
	}

	public VisualLogger withDotExecutable(String executablePath) {
		this.executablePath = executablePath;
		return this;
	}

	public VisualLogger setSaveDesignSpace() {
		this.isSaveDesignSpace = true;
		return this;
	}

	public VisualLogger setSaveStates() {
		this.isSaveStates = true;
		return this;
	}

	@Override
	public void init(ModelStore store) {
		ModelStore modelStore;
		modelStore = store;
		model = modelStore.createEmptyModel();
		for (var symbol : modelStore.getSymbols()) {
			var arity = symbol.arity();
			if (arity < 1 || arity > 2) {
				continue;
			}
			var interpretation = (Interpretation<?>) model.getInterpretation(symbol);
			allInterpretations.put(symbol, interpretation);
		}
		if (isDotRequired()) {
			if (executablePath == null) {
				throw new IllegalStateException("Dot executable path is required for output formats other than .DOT");
			}

			try {
				Process process = new ProcessBuilder(executablePath).start();
				OutputStream osToProcess = process.getOutputStream();
				PrintWriter pwToProcess = new PrintWriter(osToProcess);
				pwToProcess.close();
			} catch (IOException e) {
				throw new IllegalStateException("Could not find dot executable at " + executablePath);
			}
		}
	}

	private boolean isDotRequired() {
		if (formats.isEmpty()) {
			return false;
		}
		if (formats.size() == 1) {
			return !formats.contains(FileFormat.DOT);
		}
		return true;
	}

	@Override
	public void flush() {
		File filePath = new File(outputPath);
		filePath.mkdirs();
		if (isSaveStates) {
			saveStates();
		}
		if (isSaveDesignSpace) {
			saveDesignSpace();
		}
	}

	@Override
	public void logState(Version state, String label) {
		if (states.containsKey(state)) {
			return;
		}
		var stateId = numberOfStates++;
		states.put(state, stateId);
		designSpaceBuilder.append(stateId);
		designSpaceBuilder.append(" [label = \"").append(stateId);
		if (label != null) {
			designSpaceBuilder.append(" (");
			designSpaceBuilder.append(label);
			designSpaceBuilder.append(")");
		}
		designSpaceBuilder.append("\"\n").append("URL=\"./").append(stateId).append(".svg\"]\n");
	}

	@Override
	public void logState(VersionWithObjectiveValue stateWithObjective, String label) {
		var state = stateWithObjective.version();
		var objectiveValue = stateWithObjective.objectiveValue();

		if (label != null) {
			logState(state, "(" + objectiveValue + ") " + label);
		} else {
			logState(state, objectiveValue.toString());
		}

	}

	@Override
	public void logSolution(Version state) {
		designSpaceBuilder.append(states.get(state)).append(" [peripheries = 2]\n");
	}

	@Override
	public void logTransition(Version from, Version to, String label) {
		var fromDepth = depths.getIfAbsentPut(from, 0);
		if (fromDepth == 0) {
			depths.put(from, 0);
		}
		var toDepth = depths.getIfAbsent(to, fromDepth + 1);
		depths.put(to, Math.min(toDepth, fromDepth + 1));
		designSpaceBuilder.append(states.get(from)).append(" -> ").append(states.get(to));
		designSpaceBuilder.append(" [label=\"");
		if (label != null) {
			designSpaceBuilder.append(transitionCounter++).append(": ").append(label);
		}
		designSpaceBuilder.append("\"]\n");
	}

	private void saveStates()  {
		for (var entry : states.entrySet()) {
			var stateId = entry.getValue();
			var stateDot = createDotForModelState(entry.getKey());
			for (var format : formats) {
				if (format == FileFormat.DOT) {
					saveDot(stateDot, outputPath + "/" + stateId + ".dot");
				}
				else {
					renderDot(stateDot, format, outputPath + "/" + stateId + "." + format.getFormat());
				}
			}
		}
	}

	private void saveDesignSpace()  {
		var designSpaceDot = buildDesignSpaceDot();
		for (var format : formats) {
			if (format == FileFormat.DOT) {
				saveDot(designSpaceDot, outputPath + "/designSpace.dot");
			}
			else {
				renderDot(designSpaceDot, format, outputPath + "/designSpace." + format.getFormat());
			}
		}
	}

	private String createDotForModelState(Version version) {
		model.restore(version);


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

	private boolean saveDot(String dot, String filePath) {
		File file = new File(filePath);

		try (FileWriter writer = new FileWriter(file)) {
			writer.write(dot);
		} catch (Exception e) {
			return false;
		}
		return true;
	}

	private boolean renderDot(String dot, FileFormat format, String filePath) {
		if (isDotRequired() && executablePath == null) {
			throw new IllegalStateException("Dot executable path is required for output formats other than .DOT");
		}
		try {
			Process process = new ProcessBuilder(executablePath, "-T" + format.getFormat(), "-o", filePath).start();

			OutputStream osToProcess = process.getOutputStream();
			PrintWriter pwToProcess = new PrintWriter(osToProcess);
			pwToProcess.write(dot);
			pwToProcess.close();
			process.waitFor();
		} catch (IOException | InterruptedException e) {
			return false;
		}
        return true;
	}

	private String buildDesignSpaceDot() {
        return """
				digraph designSpace {
				nodesep=0
				ranksep=0
				node[
				\tstyle=filled
				\tfillcolor=white
				]
				""" + designSpaceBuilder + "}";
	}
}
