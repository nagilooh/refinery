package tools.refinery.generator.gui;

import org.eclipse.collections.api.map.primitive.ObjectIntMap;
import tools.refinery.language.model.problem.Node;
import tools.refinery.language.semantics.ProblemTrace;
import tools.refinery.logic.term.truthvalue.TruthValue;
import tools.refinery.store.model.Interpretation;
import tools.refinery.store.model.Model;
import tools.refinery.store.reasoning.ReasoningAdapter;
import tools.refinery.store.reasoning.interpretation.AnyPartialInterpretation;
import tools.refinery.store.reasoning.interpretation.PartialInterpretation;
import tools.refinery.store.reasoning.literal.Concreteness;
import tools.refinery.store.reasoning.representation.AnyPartialSymbol;
import tools.refinery.store.reasoning.representation.PartialSymbol;
import tools.refinery.store.reasoning.translator.MissingInterpretation;
import tools.refinery.store.representation.AnySymbol;
import tools.refinery.store.tuple.Tuple;

import com.github.weisj.jsvg.SVGDocument;
import com.github.weisj.jsvg.parser.SVGLoader;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.util.*;

public class Visualizer {

	private static final Map<Object, String> truthValueToDot = Map.of(
			TruthValue.TRUE, "1",
			TruthValue.FALSE, "0",
			TruthValue.UNKNOWN, "½",
			TruthValue.ERROR, "E",
			true, "1",
			false, "0"
	);

	public static SVGDocument renderModel(Model model, ProblemTrace trace) {
		var modelDot = createDotForModel(model, trace, null);
		return renderDotToSvg(modelDot);
	}

	public static SVGDocument renderModel(Model model, ProblemTrace trace, Activation activation) {
		var modelDot = createDotForModel(model, trace, activation);
		return renderDotToSvg(modelDot);
	}

	private static SVGDocument renderDotToSvg(String dot) {
		try {
			Process process = new ProcessBuilder("dot", "-Tsvg").start();

			// Write dot string to dot's stdin
			OutputStream osToProcess = process.getOutputStream();
			PrintWriter pwToProcess = new PrintWriter(osToProcess);
			pwToProcess.write(dot);
			pwToProcess.close();

			// Read resulting SVG from dot's stdout
			SVGLoader loader = new SVGLoader();
			SVGDocument svgDoc = loader.load(process.getInputStream());
			process.waitFor();
			return svgDoc;
		} catch (IOException | InterruptedException e) {
			e.printStackTrace();
			return null;
		}
	}

	public static String createDotForModel(Model model, ProblemTrace trace, Activation activation) {
		var unaryTupleToInterpretationsMap = new HashMap<Tuple, LinkedHashSet<PartialInterpretation<?, ?>>>();
		var reasoningAdapter = model.getAdapter(ReasoningAdapter.class);
		var relationTrace = trace.getRelationTrace();
		Map<AnyPartialSymbol, AnyPartialInterpretation> allPartialInterpretations = HashMap.newHashMap(relationTrace.size());
		for (var partialSymbol : relationTrace.values()) {
			allPartialInterpretations.put(partialSymbol,
					reasoningAdapter.getPartialInterpretation(Concreteness.PARTIAL, partialSymbol));
		}

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

		for (var entry : allPartialInterpretations.entrySet()) {
			var key = entry.getKey();
			var arity = key.arity();
			var partialInterpretation = (PartialInterpretation<?, ?>) entry.getValue();
			if (partialInterpretation instanceof MissingInterpretation) {
				continue;
			}
			var cursor = partialInterpretation.getAll();
			if (arity == 1) {
				while (cursor.move()) {
					unaryTupleToInterpretationsMap.computeIfAbsent(cursor.getKey(), k -> new LinkedHashSet<>())
							.add(partialInterpretation);
				}
			} else if (arity == 2) {
				while (cursor.move()) {
					var tuple = cursor.getKey();
					for (var i = 0; i < tuple.getSize(); i++) {
						var id = tuple.get(i);
						unaryTupleToInterpretationsMap.computeIfAbsent(Tuple.of(id), k -> new LinkedHashSet<>());
					}
					sb.append(drawEdge(cursor.getKey(), key, partialInterpretation));
				}
			}
		}
		for (var entry : unaryTupleToInterpretationsMap.entrySet()) {
			var isActivation = false;
			if (activation != null) {
				var node = entry.getKey().get(0);
				var activationSize = activation.tuple().getSize();
				for (int i = 0; i < activationSize; i++) {
					if (activation.tuple().get(i) == node) {
						System.out.println("Node " + node + " is part of activation " + activation);
						isActivation = true;
						break;
					}
				}
			}
			sb.append(drawElement(entry, isActivation, trace));
		}
		sb.append("}");
		return sb.toString();

	}

	private static StringBuilder drawElement(Map.Entry<Tuple, LinkedHashSet<PartialInterpretation<?, ?>>> entry,
											 boolean isActivation, ProblemTrace trace) {
		var sb = new StringBuilder();

		var tableStyle =  " CELLSPACING=\"0\" BORDER=\"2\" CELLBORDER=\"0\" CELLPADDING=\"4\" STYLE=\"ROUNDED\"";

		var key = entry.getKey();
		var id = key.get(0);
		var mainLabel = id + ": " + trace.getNodeName(id);
		var interpretations = entry.getValue();
		var backgroundColor = "#ffffff";
		if (isActivation) {
			backgroundColor = "#aaaaff";
		}

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
				var symbol = interpretation.getPartialSymbol();

				if (symbol.abstractDomain().concreteType() == String.class) {
					value = "\"" + value + "\"";
				}
				sb.append("\t\t<TR><TD><FONT COLOR=\"").append(color).append("\">")
						.append(interpretation.getPartialSymbol().name())
						.append("</FONT></TD><TD><FONT COLOR=\"").append(color).append("\">")
						.append("=</FONT></TD><TD><FONT COLOR=\"").append(color).append("\">").append(value)
						.append("</FONT></TD></TR>\n");
			}
		}
		sb.append("\t\t</TABLE>>\n");
		sb.append("]\n");

		return sb;
	}

	private static String drawEdge(Tuple edge, AnyPartialSymbol symbol, PartialInterpretation<?, ?> interpretation) {
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
}
