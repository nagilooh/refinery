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
	static List<String> ignored = Arrays.asList(
			"entryInRegion",
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
			"computed"
	);

	public static void main(String[] args) throws IOException {
		Path dir = Paths.get("modified-generated-models-with-extra-90-percent");

		if (!Files.isDirectory(dir)) {
			System.err.println("Directory not found: " + dir);
			return;
		}

//		Warmup
		try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir, "*.problem")) {
			int i = 0;
			for (Path path : stream) {
				System.out.println("Processing file: " + path.getFileName());

				var withUP = count(path, true);
				var noUP = count(path, false);
				System.out.println("Warmup result for file: " + path.getFileName() + " with UP: " + withUP.unknownCount() + " unknowns, parse time: " + withUP.parseTime() + "ms, init time: " + withUP.initTime() + "ms");
				Thread.sleep(200);
				if (++i >= 10) {
					break;
				}
			}
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		}

		try {
			Thread.sleep(200);
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		}

		try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir, "*.problem")) {
			for (Path path : stream) {
				System.out.println("Processing file: " + path.getFileName());

				var withUP = count(path, true);
				var noUP = count(path, false);


				// append result for this file to CSV
				Path csv = Paths.get("unknown_counts_up_90_percent-2026-03-15.csv");
				if (!Files.exists(csv)) {
					Files.writeString(csv, "file,parseTime,initTime,allCount,unknownCount,parseUPTime,initUPTime," +
							"allUPCount,unknownUPCount\n");
				}
				String csvLine =
						path.getFileName().toString() + "," + noUP.parseTime() + "," + noUP.initTime() + "," + noUP.allCount() + "," + noUP.unknownCount() + "," +
								withUP.parseTime() + "," + withUP.initTime() + "," + withUP.allCount() + "," + withUP.unknownCount() + System.lineSeparator();
				Files.writeString(csv, csvLine, java.nio.file.StandardOpenOption.CREATE, java.nio.file.StandardOpenOption.APPEND);
				Thread.sleep(200);
			}
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		}
	}

	private static CountResult count(Path path, boolean useUP) throws IOException {
		System.out.println("Counting unknowns for file: " + path.getFileName() + " with UP: " + useUP);

		var parseStart = System.currentTimeMillis();
		Problem parsedProblem = StandaloneRefinery.getProblemLoader().loadFile(path.toString());
		var parseEnd = System.currentTimeMillis();
		var parseTime = parseEnd - parseStart;
		var initStart = System.currentTimeMillis();
		ModelSemantics semantics = StandaloneRefinery.getSemanticsFactory().tryCreateSemantics(parsedProblem,
				useUP);
		var initEnd = System.currentTimeMillis();
		var initTime = initEnd - initStart;
		var trace = semantics.getProblemTrace();

		int unknownCount = 0;
		int allCount = 0;

		for (var entry : trace.getRelationTrace().entrySet()) {
			var name = entry.getKey().getName();
			if (ignored.contains(name)) {
				continue;
			}
//			System.out.println(name);
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
		return new CountResult(parseTime, initTime, allCount, unknownCount);
	}
}
