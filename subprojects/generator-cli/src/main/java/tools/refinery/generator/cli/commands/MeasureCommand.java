/*
 * SPDX-FileCopyrightText: 2023-2024 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.generator.cli.commands;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.google.inject.Inject;
import tools.refinery.generator.ModelGeneratorFactory;
import tools.refinery.generator.cli.RefineryCli;
import tools.refinery.generator.cli.utils.CliProblemLoader;
import tools.refinery.generator.cli.utils.CliProblemSerializer;
import tools.refinery.generator.cli.utils.CliUtils;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Parameters(commandDescription = "Measure the generation of a model from a partial model")
public class MeasureCommand implements Command {
	private final CliProblemLoader loader;
	private final ModelGeneratorFactory generatorFactory;
	private final CliProblemSerializer serializer;

	DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");


	private String inputPath;
	private List<String> scopes = new ArrayList<>();
	private List<String> overrideScopes = new ArrayList<>();
	private long timeout = 30;
	private int runs = 1;
	private int warmupTime = 5;
	private boolean generateUp = false;

	private String outputFolder;
	private boolean saveModels = false;

	private List<String> header = new ArrayList<>(Arrays.asList("timestamp", "measurement-type", "input",
			"scope", "generate-up", "iteration", "timeout", "parse-time", "init-time", "generation-time",
			"exploration-time",
			"generation-result"));

	@Inject
	public MeasureCommand(CliProblemLoader loader, ModelGeneratorFactory generatorFactory,
                          CliProblemSerializer serializer) {
		this.loader = loader;
		this.generatorFactory = generatorFactory;
		this.serializer = serializer;
	}

	@Parameter(description = "input path", required = true)
	public void setInputPath(String inputPath) {
		this.inputPath = inputPath;
	}

	@Parameter(names = {"-scope", "-s"}, description = "Extra scope constraints")
	public void setScopes(List<String> scopes) {
		this.scopes = scopes;
	}

	@Parameter(names = {"-scope-override", "-S"}, description = "Override scope constraints")
	public void setOverrideScopes(List<String> overrideScopes) {
		this.overrideScopes = overrideScopes;
	}

	@Parameter(names = {"-timeout", "-t"}, description = "Timeout in seconds for each generation (default: 30)")
	public void setTimeout(long timeout) {
		this.timeout = timeout;
	}

	@Parameter(names = {"-runs", "-r"}, description = "Number of runs per configuration (default: 1)")
	public void setRuns(int runs) {
		this.runs = runs;
	}

	@Parameter(names = {"-warmuptime", "-w"}, description = "Warmup time in seconds (default: 5)")
	public void setWarmupTime(int warmupTime) {
		this.warmupTime = warmupTime;
	}

	@Parameter(names = {"-generate-up", "-u"}, description = "Whether to generate UP rules (default: false)")
	public void setGenerateUp(boolean generateUp) {
		this.generateUp = generateUp;
	}

	@Parameter(names = {"-output", "-o"}, description = "Output folder", required = true)
	public void setOutputFolder(String outputFolder) {
		this.outputFolder = outputFolder;
	}

	@Parameter(names = {"-save"}, description = "Save generated models (default: false)")
	public void setSaveModels(boolean saveModels) {
		this.saveModels = saveModels;
	}

	@Override
	public int run() throws IOException {
		Date date = new  Date();
		date.getTime();
		String timestamp = LocalDateTime.now().format(formatter);
		var saveFolder = outputFolder + "/generated-models";
		var inputFileName = inputPath.split("/")[inputPath.split("/").length - 1];
		var outputFile = saveFolder + "/" + (generateUp ? "up-" : "") + inputFileName;

		var config = new RunConfiguration( String.join(",", scopes), inputPath, outputFile, timeout, generateUp, runs);

		var warmupResults = new ArrayList<MeasurementResult>();
		var measurementResults = new ArrayList<MeasurementResult>();

		String csvPathWarmup = outputFolder + "/measurement_warmup_" + timestamp + ".csv";
		String csvPath = outputFolder + "/measurement_" + timestamp + ".csv";

		Files.createDirectories(Paths.get(outputFolder));
		Files.createDirectories(Paths.get(saveFolder));

		if (csvPathWarmup != null && !(new File(csvPathWarmup).exists() && new File(csvPathWarmup).length() > 0)) {
			printHeaderToCsv(header, csvPathWarmup);
		}

		if (csvPath != null && !(new File(csvPath).exists() && new File(csvPath).length() > 0)) {
			printHeaderToCsv(header, csvPath);
		}

//		var timedOut = new HashMap<String, Integer>();
//			if (timedOut.containsKey(config.name() + "_" + config.generateUp()) && timedOut.get(config.name() + "_" + config.generateUp()) <= config.size()) {
//				System.out.println("Skipping measurement due to previous timeout: " + config.name() + " size " + config.size());
//				continue;
//			}
		var warmupStart = System.currentTimeMillis();
		var warmupIteration = 0;
		while (System.currentTimeMillis() - warmupStart < TimeUnit.SECONDS.toMillis(warmupTime) && warmupIteration < 5) {
			var warmupConfig = new RunConfiguration(config.scope(), config.input(), null, 10, config.generateUp(), 1);
			var result = runMeasurement(warmupConfig, warmupIteration++);
			warmupResults.add(result);
			printToCsv(warmupIteration, result, MeasurementType.WARMUP, csvPathWarmup);
			printToConsole(warmupIteration, result, MeasurementType.WARMUP);

		}
		for (int i = 0; i < config.count(); i++) {
//			System.out.println("Running measurement: " + config.input() + " scope " + config.scope() + " iteration " + (i + 1));
			String pathWithIndex = null;
			if (outputFolder != null) {
				pathWithIndex = CliUtils.getFileNameWithIndex(config.output(), i + 1);
			}
			var result = runMeasurement(config, i, pathWithIndex);
			measurementResults.add(result);
//				if (result.generatorResult() == GeneratorResult.TIMEOUT) {
//					timedOut.put(config.name() + "_" + config.generateUp(), config.size());
//				}
			printToCsv(i, result, MeasurementType.MEASUREMENT, csvPath);
			printToConsole(i, result, MeasurementType.MEASUREMENT);
		}
		return RefineryCli.EXIT_SUCCESS;
	}

	private MeasurementResult runMeasurement(RunConfiguration config, int randomSeed) throws IOException {
		return runMeasurement(config, randomSeed, null);
	}

	private MeasurementResult runMeasurement(RunConfiguration config, int randomSeed, String outputPath) throws IOException {
		var timestamp = LocalDateTime.now().format(formatter);
		var parseStart = System.currentTimeMillis();
		var problem = loader.loadProblem(config.input(), scopes, overrideScopes);
		var parseEnd = System.currentTimeMillis();
		var parseTime = (parseEnd - parseStart);
//		System.out.println("Parsing time: " + parseTime);

		var initStart = System.currentTimeMillis();
		var generator = generatorFactory.createGenerator(problem, config.generateUp());
		generator.setRandomSeed(randomSeed);
		var initEnd = System.currentTimeMillis();
		var initTime = (initEnd - initStart);
//		System.out.println("Initialization time: " + initTime);

		generator.setMaxNumberOfSolutions(1);
		var generationStart = System.currentTimeMillis();
		var generationResult = generator.tryGenerateWithTimeout(config.timeout(), TimeUnit.SECONDS);
		var generationEnd = System.currentTimeMillis();
		var generationTime = (generationEnd - generationStart);
//		System.out.println("Generation time: " + generationTime);
		if (saveModels && outputPath != null && generator.isLastGenerationSuccessful()) {
//			System.out.println("Saving model to " + outputPath);
			serializer.saveModel(generator, outputPath, false);
		}
		try {
			Thread.sleep(200);
		} catch (InterruptedException e) {
			throw new RuntimeException(e);
		}
		generator.close();
		var generationTimes = generator.getGenerationTimes();
		if (generationTimes.size() > 1) {
			throw new IllegalStateException("Expected only one generation time");
		}
		return new MeasurementResult(timestamp, config, parseTime, initTime, generationTime,
				generator.getGenerationTimes().get(0), generationResult);
	}

	private void printHeaderToCsv(List<String> header, String csvPath) throws IOException {
		File csvOutputFile = new File(csvPath);
		try (FileWriter fw = new FileWriter(csvOutputFile, true)) {
			fw.write(String.join(",", header));
			fw.write("\n");
		}
	}

	private void printToCsv(int iteration, MeasurementResult result, MeasurementType measurementType, String csvPath) throws IOException {
		File csvOutputFile = new File(csvPath);
		try (FileWriter fw = new FileWriter(csvOutputFile, true)) {
			fw.write(resultToString(iteration, result, measurementType));
			fw.write("\n");
		}
	}

	private void printToConsole(int iteration, MeasurementResult result, MeasurementType measurementType) {
		System.out.println(resultToString(iteration, result, measurementType));
	}

	private String resultToString(int iteration, MeasurementResult result, MeasurementType measurementType) {
		var config = result.config();
		return String.join(",", result.timestamp(), measurementType.name(), config.input(),
				config.scope(), String.valueOf(config.generateUp()), String.valueOf(iteration),
				java.lang.String.valueOf(config.timeout()), String.valueOf(result.parsingTime()),
				java.lang.String.valueOf(result.initializationTime()), String.valueOf(result.generationTime()),
				java.lang.String.valueOf(result.explorationTime()), result.generatorResult().name());
	}
}

