/*
 * SPDX-FileCopyrightText: 2023-2024 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.generator.manual;

import com.google.inject.Inject;
import org.eclipse.collections.api.factory.primitive.IntObjectMaps;
import org.eclipse.collections.api.map.primitive.MutableIntObjectMap;
import org.eclipse.xtext.naming.IQualifiedNameProvider;
import tools.refinery.language.model.problem.*;
import tools.refinery.language.semantics.NodeNameProvider;
import tools.refinery.language.semantics.ProblemTrace;
import tools.refinery.language.semantics.SemanticsUtils;
import tools.refinery.logic.term.truthvalue.TruthValue;
import tools.refinery.store.model.Model;
import tools.refinery.store.reasoning.ReasoningAdapter;
import tools.refinery.store.reasoning.interpretation.PartialInterpretation;
import tools.refinery.store.reasoning.literal.Concreteness;
import tools.refinery.store.reasoning.representation.PartialRelation;
import tools.refinery.store.reasoning.translator.typehierarchy.InferredType;
import tools.refinery.store.reasoning.translator.typehierarchy.TypeHierarchyTranslator;
import tools.refinery.store.tuple.Tuple;

import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

public class NodeNamer {

	@Inject
	private NodeNameProvider nameProvider;

	private ProblemTrace trace;
	private Model model;
	private PartialInterpretation<TruthValue, Boolean> existsInterpretation;
	private Problem problem;
	private final MutableIntObjectMap<Node> nodes = IntObjectMaps.mutable.empty();


	public MutableIntObjectMap<Node> nameNodes(ProblemTrace trace, Model model) {
		this.trace = trace;
		this.model = model;
		ReasoningAdapter reasoningAdapter = model.getAdapter(ReasoningAdapter.class);
		existsInterpretation = reasoningAdapter.getPartialInterpretation(Concreteness.CANDIDATE,
				ReasoningAdapter.EXISTS_SYMBOL);
		problem = trace.getProblem();
		problem.getStatements().removeIf(NodeNamer::shouldRemoveStatement);
		nameProvider.setProblem(problem);
		addClassAssertions();
		return nodes;
	}

	private static boolean shouldRemoveStatement(Statement statement) {
		return statement instanceof Assertion || statement instanceof ScopeDeclaration;
	}

	private void addClassAssertions() {
		var types = trace.getMetamodel().typeHierarchy().getPreservedTypes().keySet().stream()
				.collect(Collectors.toMap(Function.identity(), trace::getRelation));
		var cursor = model.getInterpretation(TypeHierarchyTranslator.TYPE_SYMBOL).getAll();
		while (cursor.move()) {
			var key = cursor.getKey();
			var nodeId = key.get(0);
			if (isExistingNode(nodeId)) {
				createNodeAndAssertType(nodeId, cursor.getValue(), types);
			}
		}
	}

	private void createNodeAndAssertType(int nodeId, InferredType inferredType, Map<PartialRelation, Relation> types) {
		var candidateTypeSymbol = inferredType.candidateType();
		var candidateRelation = types.get(candidateTypeSymbol);
		if (candidateRelation instanceof EnumDeclaration) {
			// Type assertions for enum literals are added implicitly.
			return;
		}
		Node node = nodes.get(nodeId);
		if (node == null) {
			var typeName = candidateRelation.getName();
			var nodeName = nameProvider.getNextName(typeName);
			node = ProblemFactory.eINSTANCE.createNode();
			node.setName(nodeName);
			nodes.put(nodeId, node);
		}
	}

	private boolean isExistingNode(int nodeId) {
		var exists = existsInterpretation.get(Tuple.of(nodeId));
		if (!exists.isConcrete()) {
			throw new IllegalStateException("Invalid EXISTS %s for node %d".formatted(exists, nodeId));
		}
		return exists.may();
	}
}
