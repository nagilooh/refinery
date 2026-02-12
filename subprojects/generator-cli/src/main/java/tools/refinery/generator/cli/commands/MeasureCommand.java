/*
 * SPDX-FileCopyrightText: 2023-2024 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.generator.cli.commands;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.google.inject.Inject;
import tools.refinery.generator.GeneratorResult;
import tools.refinery.generator.ModelGeneratorFactory;
import tools.refinery.generator.cli.RefineryCli;
import tools.refinery.generator.cli.utils.CliProblemLoader;
import tools.refinery.generator.cli.utils.CliProblemSerializer;
import tools.refinery.generator.cli.utils.CliUtils;

import java.io.*;
import java.nio.charset.StandardCharsets;
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

	private String configPath;
	private String inputFolder;
	private String outputFolder;
	private boolean saveModels = false;

	private List<String> header = new ArrayList<>(Arrays.asList("timestamp", "measurement-type", "name", "input",
			"generate-up",
			"timeout", "parse-time", "init-time", "generation-time", "exploration-time", "generation-result"));

	@Inject
	public MeasureCommand(CliProblemLoader loader, ModelGeneratorFactory generatorFactory,
                          CliProblemSerializer serializer) {
		this.loader = loader;
		this.generatorFactory = generatorFactory;
		this.serializer = serializer;
	}

	@Parameter(description = "config path", required = true)
	public void setConfigPath(String configPath) {
		this.configPath = configPath;
	}

	@Parameter(names = {"-input", "-i"}, description = "Input folder", required = true)
	public void setInputFolder(String inputFolder) {
		this.inputFolder = inputFolder;
	}

	@Parameter(names = {"-output", "-o"}, description = "Output folder", required = true)
	public void setOutputFolder(String outputFolder) {
		this.outputFolder = outputFolder;
	}

	@Parameter(names = {"-save", "-s"}, description = "Save models")
	public void setSaveModels(boolean saveModels) {
		this.saveModels = saveModels;
	}

	@Override
	public int run() throws IOException {
		Date date = new  Date();
		date.getTime();
		String timestamp = LocalDateTime.now().format(formatter);
		var saveFolder = outputFolder + "/generated-models";



		var configFile = new File(configPath);
		InputStream inputStream = new FileInputStream(configFile);
		InputStreamReader streamReader = new InputStreamReader(inputStream, StandardCharsets.UTF_8);
		BufferedReader reader = new BufferedReader(streamReader);
		var headerLine = reader.readLine(); // skip header

		var runConfigs = new ArrayList<RunConfiguration>();

		var maxCount = 0;
		for (String line; (line = reader.readLine()) != null;) {
			var columns = line.split(",");
			var name = columns[0];
			var size = Integer.parseInt(columns[1]);
			var inputPath = inputFolder + "/" + columns[2];
			var timeout = Long.parseLong(columns[3]);
			var generateUP = Boolean.parseBoolean(columns[4]);
			var count = Integer.parseInt(columns[5]);
			var outputFile = saveFolder + "/" + (generateUP ? "up-" : "") + columns[2];
			maxCount = Math.max(maxCount, count);
			runConfigs.add(new RunConfiguration(name, size,inputPath, outputFile,timeout,generateUP,count));
		}

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
		for (var config : runConfigs) {
//			if (timedOut.containsKey(config.name() + "_" + config.generateUp()) && timedOut.get(config.name() + "_" + config.generateUp()) <= config.size()) {
//				System.out.println("Skipping measurement due to previous timeout: " + config.name() + " size " + config.size());
//				continue;
//			}
//			var warmupStart = System.currentTimeMillis();
//			while (System.currentTimeMillis() - warmupStart < TimeUnit.SECONDS.toMillis(5)) {
//				var result = runMeasurement(config, config.count() + 1);
//				warmupResults.add(result);
//				printToCsv(result, MeasurementType.WARMUP, csvPathWarmup);
//			}
//			for (int i = 0; i < config.count(); i++) {
//				System.out.println("Running measurement: " + config.name() + " size " + config.size() + " iteration " + (i + 1));
//				String pathWithIndex = null;
//				if (outputFolder != null) {
//					pathWithIndex = CliUtils.getFileNameWithIndex(config.output(), i + 1);
//				}
//				var result = runMeasurement(config, i, pathWithIndex);
//				measurementResults.add(result);
////				if (result.generatorResult() == GeneratorResult.TIMEOUT) {
////					timedOut.put(config.name() + "_" + config.generateUp(), config.size());
////				}
//				printToCsv(result, MeasurementType.MEASUREMENT, csvPath);
//			}
			var i = 0;
			var seed = 0;
			while (i < config.count()) {
				System.out.println("Running measurement: " + config.name() + " size " + config.size() + " iteration " + (i + 1));
				String pathWithIndex = null;
				if (outputFolder != null) {
					pathWithIndex = CliUtils.getFileNameWithIndex(config.output(), i + 1);
				}
				var result = runMeasurement(config, seed++, pathWithIndex);
				measurementResults.add(result);
				if (result.generatorResult() != GeneratorResult.TIMEOUT) {
					i++;
				}
//				if (result.generatorResult() == GeneratorResult.TIMEOUT) {
//					timedOut.put(config.name() + "_" + config.generateUp(), config.size());
//				}
				printToCsv(result, MeasurementType.MEASUREMENT, csvPath);
			}
		}

		return RefineryCli.EXIT_SUCCESS;
	}

	private MeasurementResult runMeasurement(RunConfiguration config, int randomSeed) throws IOException {
		return runMeasurement(config, randomSeed, null);
	}

	private MeasurementResult runMeasurement(RunConfiguration config, int randomSeed, String outputPath) throws IOException {
		var timestamp = LocalDateTime.now().format(formatter);
		var parseStart = System.currentTimeMillis();
		var problem = loader.loadProblem(config.input());
		var parseEnd = System.currentTimeMillis();
		var parseTime = (parseEnd - parseStart);
		System.out.println("Parsing time: " + parseTime);

		var initStart = System.currentTimeMillis();
		var generator = generatorFactory.createGenerator(problem, config.generateUp());
		generator.setRandomSeed(randomSeed);
		var initEnd = System.currentTimeMillis();
		var initTime = (initEnd - initStart);
		System.out.println("Initialization time: " + initTime);

		generator.setMaxNumberOfSolutions(1);
		var generationStart = System.currentTimeMillis();
		var generationResult = generator.tryGenerateWithTimeout(config.timeout(), TimeUnit.SECONDS);
		var generationEnd = System.currentTimeMillis();
		var generationTime = (generationEnd - generationStart);
		System.out.println("Generation time: " + generationTime);
		if (saveModels && outputPath != null && generator.isLastGenerationSuccessful()) {
			System.out.println("Saving model to " + outputPath);
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

	private void printToCsv(MeasurementResult result, MeasurementType measurementType, String csvPath) throws IOException {
		var config = result.config();
		File csvOutputFile = new File(csvPath);
		try (FileWriter fw = new FileWriter(csvOutputFile, true)) {
			fw.write(String.join(",", result.timestamp(), measurementType.name(), config.name(), config.input(),
					String.valueOf(config.generateUp()), String.valueOf(config.timeout()),
					String.valueOf(result.parsingTime()), String.valueOf(result.initializationTime()),
					String.valueOf(result.generationTime()), String.valueOf(result.explorationTime()),
					result.generatorResult().name()));
			fw.write("\n");
		}
	}
}

