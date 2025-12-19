package tools.refinery.measurement;

import tools.refinery.generator.ModelSemantics;
import tools.refinery.generator.standalone.StandaloneRefinery;
import tools.refinery.language.model.problem.Problem;
import tools.refinery.logic.term.truthvalue.TruthValue;
import tools.refinery.store.reasoning.representation.PartialRelation;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

public class CountUnknown {
	static List<String> ignored = Arrays.asList("entryInRegion," +
					"noEntryInRegion",
			"multipleEntryInRegion",
			"incomingToEntry",
			"noOutgoingTransitionFromEntry",
			"multipleTransitionFromEntry",
			"outgoingFromExit",
			"outgoingFromFinal",
			"stateInRegion",
			"noStateInRegion",
			"choiceHasNoOutgoing",
			"choiceHasNoIncoming",
			"sameStraightDiverging",
			"noDiverging",
			"noStraight",
			"tooManyConncetions",
			"selfLoop",
			"TrackElement",
			"underconnectedSwitch",
			"switchWithTwoConnections",
			"sensorWithoutElement",
			"sensorMonitorsMultipleSwitches",
			"duplicateMonitors",
			"emptyRegion",
			"posLength",
			"switchMonitored",
			"routeSensor",
			"switchSet",
			"connectedSegments",
			"semaphoreNeighbor",
			"HC1",
			"HC2c",
			"HC2",
			"SC1",
			"hasCancle",
			"SC2",
			"computed");

	public static void main(String[] args) throws IOException {
		Path dir = Paths.get("test_output");

		if (!Files.isDirectory(dir)) {
			System.err.println("Directory not found: " + dir);
			return;
		}

		try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir, "*.problem")) {
			for (Path path : stream) {
				System.out.println("Processing file: " + path.getFileName());
				Problem parsedProblem = StandaloneRefinery.getProblemLoader().loadFile(path.toString());
				ModelSemantics semantics = StandaloneRefinery.getSemanticsFactory().tryCreateSemantics(parsedProblem);
				var trace = semantics.getProblemTrace();

				int unknownCount = 0;
				int allCount = 0;

				for (var entry : trace.getRelationTrace().entrySet()) {
					var name = entry.getKey().getName();
					if (ignored.contains(name)) {
						continue;
					}
					System.out.println(name);
					PartialRelation partialRelation;
					try {
						partialRelation = entry.getValue().asPartialRelation();
					} catch (IllegalStateException e) {
						continue;
					}
					var cursor = semantics.getPartialInterpretation(partialRelation).getAll();

					while (cursor.move()) {
						allCount++;
						if (cursor.getValue() == TruthValue.UNKNOWN) {
							unknownCount++;
						}
					}
				}

				System.out.println("Unknown count: " + unknownCount);

				// append result for this file to CSV
				Path csv = Paths.get("unknown_counts.csv");
				if (!Files.exists(csv)) {
				    Files.writeString(csv, "file,allCount,unknownCount\n");
				}
				String csvLine =
						path.getFileName().toString() + "," + allCount + "," + unknownCount + System.lineSeparator();
				Files.writeString(csv, csvLine, java.nio.file.StandardOpenOption.CREATE, java.nio.file.StandardOpenOption.APPEND);
			}
		}
	}
}
