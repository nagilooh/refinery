package tools.refinery.measurement;

import tools.refinery.generator.ModelSemantics;
import tools.refinery.generator.standalone.StandaloneRefinery;
import tools.refinery.language.model.problem.Problem;
import tools.refinery.logic.term.truthvalue.TruthValue;
import tools.refinery.store.reasoning.representation.PartialRelation;

import java.io.IOException;

public class CountUnknown {
	public static void main(String[] args) throws IOException {
		Problem parsedProblem = StandaloneRefinery.getProblemLoader().loadFile("test-output/solution.refinery");
		ModelSemantics semantics = StandaloneRefinery.getSemanticsFactory().tryCreateSemantics(parsedProblem);
		var trace = semantics.getProblemTrace();

		int unknownCount = 0;
		for (var entry : trace.getRelationTrace().entrySet()) {
			System.out.println(entry.getKey().getName());
			PartialRelation partialRelation;
			try {
				partialRelation = entry.getValue().asPartialRelation();

			}
			catch (IllegalStateException e) {
				continue;
			}
			var cursor = semantics.getPartialInterpretation(partialRelation).getAll();

			while (cursor.move()) {
				if (cursor.getValue() == TruthValue.UNKNOWN) {
					unknownCount++;
				}
			}
		}
		System.out.println("Unknown count: " + unknownCount);
	}
}
