/*
 * SPDX-FileCopyrightText: 2024 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.generator.cli.commands;


import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.google.inject.Inject;
import tools.refinery.generator.ModelSemantics;
import tools.refinery.generator.ModelSemanticsFactory;
import tools.refinery.generator.cli.RefineryCli;
import tools.refinery.generator.cli.utils.CliProblemLoader;
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

@Parameters(commandDescription = "Check a partial model consistency")
public class CountUnknownCommand implements Command {
	private final CliProblemLoader loader;
	private final ModelSemanticsFactory semanticsFactory;

	private String inputPath;
	private String outputFolder;

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
			"computed");

	@Inject
	public CountUnknownCommand(CliProblemLoader loader, ModelSemanticsFactory semanticsFactory) {
		this.loader = loader;
		this.semanticsFactory = semanticsFactory;
	}

	@Parameter(description = "input path", required = true)
	public void setInputPath(String inputPath) {
		this.inputPath = inputPath;
	}

	@Parameter(names = {"-output", "-o"}, description = "Output folder", required = true)
	public void setOutputFolder(String outputFolder) {
		this.outputFolder = outputFolder;
	}


	@Override
	public int run() throws IOException {

		Files.createDirectories(Paths.get(outputFolder));
//		Warmup
		for (int i = 0; i < 10; i++) {
			var noUP = count(inputPath, false);
			var withUP = count(inputPath, true);


			// append result for this file to CSV
			Path csv = Paths.get(outputFolder, "warmup_unknown_counts.csv");
			if (!Files.exists(csv)) {
				Files.writeString(csv, "file,parseTime,initTime,allCount,unknownCount,parseUPTime,initUPTime," +
						"allUPCount,unknownUPCount\n");
			}
			String csvLine =
					inputPath + "," + noUP.parseTime() + "," + noUP.initTime() + "," + noUP.allCount() + "," + noUP.unknownCount() + "," +
							withUP.parseTime() + "," + withUP.initTime() + "," + withUP.allCount() + "," + withUP.unknownCount() + System.lineSeparator();
			Files.writeString(csv, csvLine, java.nio.file.StandardOpenOption.CREATE, java.nio.file.StandardOpenOption.APPEND);
			try {
				Thread.sleep(200);
			} catch (InterruptedException e) {
				throw new RuntimeException(e);
			}
		}

		var withUP = count(inputPath, true);
		var noUP = count(inputPath, false);


		// append result for this file to CSV
		Path csv = Paths.get(outputFolder, "unknown_counts.csv");
		if (!Files.exists(csv)) {
			Files.writeString(csv, "file,parseTime,initTime,allCount,unknownCount,parseUPTime,initUPTime," +
					"allUPCount,unknownUPCount\n");
		}
		String csvLine =
				inputPath + "," + noUP.parseTime() + "," + noUP.initTime() + "," + noUP.allCount() + "," + noUP.unknownCount() + "," +
						withUP.parseTime() + "," + withUP.initTime() + "," + withUP.allCount() + "," + withUP.unknownCount() + System.lineSeparator();
		Files.writeString(csv, csvLine, java.nio.file.StandardOpenOption.CREATE, java.nio.file.StandardOpenOption.APPEND);

		return 0;
	}

	private CountResult count(String inputPath, boolean useUP) throws IOException {
		var parseStart = System.currentTimeMillis();
		Problem parsedProblem = loader.loadProblem(inputPath);
		var parseEnd = System.currentTimeMillis();
		var parseTime = parseEnd - parseStart;
		var initStart = System.currentTimeMillis();
		try (var semantics = semanticsFactory.createSemantics(parsedProblem, useUP)) {
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
}
