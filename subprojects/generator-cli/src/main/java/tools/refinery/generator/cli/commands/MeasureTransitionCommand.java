/*
 * SPDX-FileCopyrightText: 2023-2024 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.generator.cli.commands;

import com.beust.jcommander.Parameter;
import com.beust.jcommander.Parameters;
import com.google.inject.Injector;
import tools.refinery.generator.GeneratorResult;
import tools.refinery.generator.cli.RefineryCli;
import tools.refinery.generator.cli.utils.CliUtils;
import tools.refinery.language.ProblemStandaloneSetup;
import tools.refinery.measurement.Abstraction;
import tools.refinery.measurement.MeasurementCase;
import tools.refinery.measurement.MeasurementDomain;
import tools.refinery.measurement.MeasurementScenario;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Parameters(commandDescription = "Measure the generation of a model from a partial model")
public class MeasureTransitionCommand implements Command {
	DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");

	private String inputPath;
	private int runs = 1;
	private int warmupTime = 5;
	private int steps = 1;
	private int size = 2;

	private String outputFolder;
	private MeasurementDomain domain;
	private MeasurementScenario scenario;
	private Abstraction abstraction;

	private Injector injector;

	private List<String> header = new ArrayList<>(Arrays.asList(
			"timestamp",
			"measurement-type",
			"domain",
			"scenario",
			"size",
			"steps",
			"iteration",
			"initializationTime",
			"queryTimes(avg)",
			"queryMatches(avg)",
			"transitionTimes(avg)",
			"propagationTimes(avg)",
			"flushTimes(avg)"
	));


	@Parameter(names = {"-runs", "-r"}, description = "Number of runs per configuration (default: 1)")
	public void setRuns(int runs) {
		this.runs = runs;
	}

	@Parameter(names = {"-steps", "-c"}, description = "Number of steps per run (default: 1)")
	public void setSteps(int steps) {
		this.steps = steps;
	}

	@Parameter(names = {"-size", "-s"}, description = "The size of the model (default: 2)")
	public void setSize(int size) {
		this.size = size;
	}

	@Parameter(names = {"-domain", "-d"}, description = "The measurement domain")
	public void setDomain(String domain) {
		this.domain = MeasurementDomain.valueOf(domain.toUpperCase());
	}

	@Parameter(names = {"-scenario", "-sc"}, description = "The measurement scenario")
	public void setScenario(String scenario) {
		this.scenario = MeasurementScenario.valueOf(scenario.toUpperCase());
	}

	@Parameter(names = {"-abstractions", "-a"}, description = "The measurement abstraction")
	public void setAbstraction(String abstraction) {
		this.abstraction = Abstraction.valueOf(abstraction.toUpperCase());
	}

	@Parameter(names = {"-warmuptime", "-w"}, description = "Warmup time in seconds (default: 5)")
	public void setWarmupTime(int warmupTime) {
		this.warmupTime = warmupTime;
	}

	@Parameter(names = {"-output", "-o"}, description = "Output folder", required = true)
	public void setOutputFolder(String outputFolder) {
		this.outputFolder = outputFolder;
	}

	@Override
	public int run() throws IOException {
		String timestamp = LocalDateTime.now().format(formatter);
		injector = new ProblemStandaloneSetup().createInjectorAndDoEMFRegistration();

		var config = new RunConfiguration(domain, scenario, abstraction, size, steps, runs);

		var warmupResults = new ArrayList<MeasurementResult>();
		var measurementResults = new ArrayList<MeasurementResult>();

		String csvPathWarmup = outputFolder + "/measurement_warmup_" + timestamp + ".csv";
		String csvPath = outputFolder + "/measurement_" + timestamp + ".csv";

		Files.createDirectories(Paths.get(outputFolder));

		if (csvPathWarmup != null && !(new File(csvPathWarmup).exists() && new File(csvPathWarmup).length() > 0)) {
			printHeaderToCsv(header, csvPathWarmup);
		}

		if (csvPath != null && !(new File(csvPath).exists() && new File(csvPath).length() > 0)) {
			printHeaderToCsv(header, csvPath);
		}

		var warmupStart = System.currentTimeMillis();
		var warmupIteration = 0;
		while (System.currentTimeMillis() - warmupStart < TimeUnit.SECONDS.toMillis(warmupTime) || warmupIteration < 5) {
			var warmupConfig = new RunConfiguration(domain, scenario,abstraction, size, 1000, 1);
			var result = runMeasurement(warmupConfig, warmupIteration++);
			warmupResults.add(result);
			printToCsv(warmupIteration, result, MeasurementType.WARMUP, csvPathWarmup);
			printToConsole(warmupIteration, result, MeasurementType.WARMUP);

		}
		for (int i = 0; i < config.runs(); i++) {
			var result = runMeasurement(config, i);
			measurementResults.add(result);
			printToCsv(i, result, MeasurementType.MEASUREMENT, csvPath);
			printToConsole(i, result, MeasurementType.MEASUREMENT);
		}
		return RefineryCli.EXIT_SUCCESS;
	}

	private MeasurementResult runMeasurement(RunConfiguration config, int randomSeed) throws IOException {
		var timestamp = LocalDateTime.now().format(formatter);
		MeasurementCase measurement;
		switch (config.domain()) {
			case STATECHART -> measurement =
					injector.getInstance(tools.refinery.measurement.StatechartMeasurement.class);
			case TRAIN_BENCHMARK -> measurement =
					injector.getInstance(tools.refinery.measurement.TrainBenchmarkMeasurement.class);
			default -> throw new IllegalArgumentException("Unknown domain: " + config.domain());
		}
		// Load the model
		var loadingStartTime = System.nanoTime();
		measurement.init(config.size(), config.scenario(), config.abstraction(), false);
		var loadingEndTime = System.nanoTime();
		var loadingTime = loadingEndTime - loadingStartTime;

		// Check the queries
		measurement.check();

		// Run the transformation + recheck loop
		var runStartTime = System.nanoTime();
		measurement.transformAndRecheck(config.steps());
		var runEndTime = System.nanoTime();
		var totalRunTime = runEndTime - runStartTime;

		measurement.finish();

		return new MeasurementResult(
				timestamp,
				config,
				loadingTime,
				measurement.getQueryTimes(),
				measurement.getQueryMatches(),
				measurement.getTransitionTimes(),
				measurement.getPropagationTimes(),
				measurement.getFlushTimes());
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
			return String.join(",",
					result.timestamp(),
					measurementType.name(),
					config.domain().name(),
					config.scenario().name(),
					String.valueOf(config.size()),
					String.valueOf(config.steps()),
					String.valueOf(iteration),
					String.valueOf(result.initializationTime()),
					String.valueOf(result.queryTimes().values().stream().flatMap(List::stream).mapToLong(Long::longValue).average().orElse(0)),
					String.valueOf(result.queryMatches().values().stream().flatMap(List::stream).mapToInt(Integer::intValue).average().orElse(0)),
					String.valueOf(result.transitionTimes().values().stream().flatMap(List::stream).mapToLong(Long::longValue).average().orElse(0)),
					String.valueOf(result.propagationTimes().values().stream().flatMap(List::stream).mapToLong(Long::longValue).average().orElse(0)),
					String.valueOf(result.flushTimes().values().stream().flatMap(List::stream).mapToLong(Long::longValue).average().orElse(0))
			);
		}
}

