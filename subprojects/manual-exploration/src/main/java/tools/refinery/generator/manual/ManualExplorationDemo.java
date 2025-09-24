/*
 * SPDX-FileCopyrightText: 2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.generator.manual;

import org.eclipse.emf.ecore.resource.Resource;
import tools.refinery.generator.ModelFacade;
import tools.refinery.generator.impl.ManualExplorationImpl;
import tools.refinery.generator.standalone.StandaloneRefinery;
import tools.refinery.language.semantics.metadata.NodeMetadataFactory;
import tools.refinery.store.model.Model;
import tools.refinery.store.reasoning.literal.Concreteness;
import tools.refinery.visualization.ModelVisualizerAdapter;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Map;

public class ManualExplorationDemo {
	static Model model;

	public static void main(String[] args) throws IOException {
		var problem = StandaloneRefinery.getProblemLoader().loadString("""
			% Metamodel

			abstract class CompositeElement {
				contains Region[] regions
			}

			class Region {
				contains Vertex[] vertices opposite region
			}

			abstract class Vertex {
				container Region region opposite vertices
				contains Transition[] outgoingTransition opposite source
				Transition[] incomingTransition opposite target
			}

			class Transition {
				container Vertex source opposite outgoingTransition
				Vertex[1] target opposite incomingTransition
			}

			abstract class Pseudostate extends Vertex.

			abstract class RegularState extends Vertex.

			class Entry extends Pseudostate.

			class Exit extends Pseudostate.

			class Choice extends Pseudostate.

			class FinalState extends RegularState.

			class State extends RegularState, CompositeElement.

			class Statechart extends CompositeElement.

			% Constraints

			%% Entry

			pred entryInRegion(Region r, Entry e) <->
				vertices(r, e).

			error noEntryInRegion(Region r) <->
				!entryInRegion(r, _).

			error multipleEntryInRegion(Region r) <->
				entryInRegion(r, e1),
				entryInRegion(r, e2),
				e1 != e2.

			error incomingToEntry(Transition t, Entry e) <->
				target(t, e).

			error noOutgoingTransitionFromEntry(Entry e) <->
				!source(_, e).

			error multipleTransitionFromEntry(Entry e, Transition t1, Transition t2) <->
				outgoingTransition(e, t1),
				outgoingTransition(e, t2),
				t1 != t2.

			%% Exit

			error outgoingFromExit(Transition t, Exit e) <->
				source(t, e).

			%% Final

			error outgoingFromFinal(Transition t, FinalState e) <->
				source(t, e).

			%% State vs Region

			pred stateInRegion(Region r, State s) <->
				vertices(r, s).

			error noStateInRegion(Region r) <->
				!stateInRegion(r, _).

			%% Choice

			error choiceHasNoOutgoing(Choice c) <->
				!source(_, c).

			error choiceHasNoIncoming(Choice c) <->
				!target(_, c).

			% Instance model

			Statechart(sct).

			% Scope

			scope node = 20..30, Region = 2..*, Choice = 1..*, Statechart += 0.

            """);
		try (var generator = StandaloneRefinery.getManualExplorationFactory().createGenerator(problem)) {
			model = generator.getModel();
			model.getAdapter(ModelVisualizerAdapter.class).visualize(generator.getLast());
			System.out.println(generator.getProblemTrace().getNodeTrace());
			var nodeTrace = generator.getProblemTrace().getNodeTrace().flipUniqueValues();
			var keySet = nodeTrace.keySet();
			for (var key : keySet.toArray()) {
				System.out.println(key + ": " + nodeTrace.get(key));
			}
			var activations = generator.getStoreManager();


			var nodeNamer = StandaloneRefinery.getInjector().getInstance(NodeNamer2.class);
			problem = nodeNamer.serializeSolution(generator.getProblemTrace(), model);
//			for (var node : nodes) {
//				System.out.println(node);
//			}
//			System.out.println();
			System.out.println(problem.getNodes());

			for (int i = 0; i < 10; i++) {
				var result = generator.step();
			}
			model.getAdapter(ModelVisualizerAdapter.class).visualize(generator.getLast());

			nodeNamer = StandaloneRefinery.getInjector().getInstance(NodeNamer2.class);
			problem = nodeNamer.serializeSolution(generator.getProblemTrace(), model);
//			for (var node : nodes) {
//				System.out.println(node);
//			}
//			System.out.println();
			System.out.println(problem.getNodes());
			ManualExplorationDemo.saveModel(generator, null, true);

			var metadata = generator.getNodesMetadata();
			System.out.println(metadata);



//			var trace = generator.getProblemTrace();
//			var verticesRelation = trace.getPartialRelation("Region");
//			var verticesInterpretation = generator.getPartialInterpretation(verticesRelation);
//			var cursor = verticesInterpretation.getAll();
//			while (cursor.move()) {
//				System.out.printf("%s: %s%n", cursor.getKey(), cursor.getValue());
//			}
		}
	}

	public static void saveModel(ModelFacade modelFacade, String outputPath,
								 boolean allowStandardOutput) throws IOException {
		var problem = modelFacade.serialize();
		var resource = problem.eResource();
		var saveOptions = Map.of();
		if (outputPath == null) {
			if (!allowStandardOutput) {
				throw new IllegalArgumentException("Refusing to save model to standard output '" + "-" + "'");
			}
			printSolution(resource, saveOptions);
		} else {
			try (var outputStream = new FileOutputStream(outputPath)) {
				resource.save(outputStream, saveOptions);
			}
		}
	}

	// We deliberately write to the standard output if no output path is specified.
	@SuppressWarnings("squid:S106")
	private static void printSolution(Resource solutionResource, Map<?, ?> saveOptions) throws IOException {
		solutionResource.save(System.out, saveOptions);
	}


}
