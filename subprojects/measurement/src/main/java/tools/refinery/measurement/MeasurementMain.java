package tools.refinery.measurement;

import tools.refinery.generator.standalone.StandaloneRefinery;

import java.io.IOException;
import java.util.List;

public class MeasurementMain {
	public static void main(String[] args) throws IOException {
		var loader = StandaloneRefinery.getProblemLoader();
		var problem = loader.loadFile("fase-trainbenchmark.problem");
//		problem = loader.loadScopeConstraints(problem, List.of(), List.of("node = 50..80"));
		try (var generator = StandaloneRefinery.getGeneratorFactory().createGenerator(problem)) {
			generator.setMaxNumberOfSolutions(1);
			for (int i = 0; i < 30; i++) {
				System.out.println(i);
				generator.generate();
				System.out.println(generator.getSolutionCount());
				System.out.println(generator.getGenerationTimes());
			}
		}
	}
}
