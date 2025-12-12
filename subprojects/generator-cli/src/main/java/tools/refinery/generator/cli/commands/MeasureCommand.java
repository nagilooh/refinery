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
import tools.refinery.generator.standalone.StandaloneRefinery;

import java.io.*;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Parameters(commandDescription = "Measure the generation of a model from a partial model")
public class MeasureCommand implements Command {
	private final CliProblemLoader loader;
	private final ModelGeneratorFactory generatorFactory;
	private final CliProblemSerializer serializer;

	private String inputPath;
	private String outputPath;
	private String csvPath;
	private List<String> scopes = new ArrayList<>();
	private List<String> overrideScopes = new ArrayList<>();
	private int count = 1;
	private boolean generateUP = false;
	private long timeout = 10L;
	private Timestamp timestamp;

	private List<String> header = new ArrayList<>(Arrays.asList("timestamp", "measurement-type", "input", "output",
			"generate-up", "timeout", "runtime"));

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

	@Parameter(names = {"-output", "-o"}, description = "Output path")
	public void setOutputPath(String outputPath) {
		this.outputPath = outputPath;
	}

	@Parameter(names = {"-csv", "-c"}, description = "Output path", required = true)
	public void setCsvPath(String csvPath) {
		this.csvPath = csvPath;
	}

	@Parameter(names = {"-scope", "-s"}, description = "Extra scope constraints")
	public void setScopes(List<String> scopes) {
		this.scopes = scopes;
	}

	@Parameter(names = {"-scope-override", "-S"}, description = "Override scope constraints")
	public void setOverrideScopes(List<String> overrideScopes) {
		this.overrideScopes = overrideScopes;
	}

	@Parameter(names = {"-solution-number", "-n"}, description = "Maximum number of solutions")
	public void setCount(int count) {
		if (count <= 0) {
			throw new IllegalArgumentException("Count must be positive");
		}
		this.count = count;
	}

	@Parameter(names = {"-generate-up", "-u"}, description = "Generate unit propagation rules")
	public void setGenerateUP(boolean generateUP) {
		this.generateUP = generateUP;
	}

	@Parameter(names = {"-timeout", "-t"}, description = "Timeout (seconds) for each generation")
	public void setTimeout(long timeout) {
		this.timeout = timeout;
	}

	@Override
	public int run() throws IOException {
		Date date = new  Date();
		date.getTime();
		timestamp = new Timestamp(date.getTime());
		var warmupInitializationConfiguration = new RunConfiguration(MeasurementType.WARMUP_INITIALIZATION,
				generateUP, inputPath, outputPath, timeout, count);
		var warmupConfiguration = new RunConfiguration(MeasurementType.WARMUP, generateUP, inputPath, outputPath,
				timeout, count);
		var initConfiguration = new RunConfiguration(MeasurementType.INITIALIZATION, generateUP, inputPath,
				outputPath, timeout, count);
		var measurementConfiguration = new RunConfiguration(MeasurementType.MEASUREMENT, generateUP, inputPath,
				outputPath, timeout, count);

		if (csvPath != null && !(new File(csvPath).exists() && new File(csvPath).length() > 0)) {
			printHeaderToCsv(header, csvPath);
		}

		var problem = loader.loadProblem(inputPath, scopes, overrideScopes);

		var start = System.currentTimeMillis();
		var generator = generatorFactory.createGenerator(problem, generateUP);
		var end = System.currentTimeMillis();
		printToCsv(initConfiguration, (end - start), csvPath);
		System.out.println("Initialization time: " + (end - start));
		generator.setMaxNumberOfSolutions(1);
		for (int i = 0; i < count; i++) {
			header.add("generation-" + (i + 1));
			System.out.println(i);
			generator.tryGenerateWithTimeout(timeout, TimeUnit.SECONDS);
			System.out.println(generator.getSolutionCount());
			System.out.println(generator.getGenerationTimes());
		}

		for (var time : generator.getGenerationTimes()) {
			printToCsv(measurementConfiguration, time, csvPath);
		}
		if (outputPath != null) {
			int solutionCount = generator.getSolutionCount();
			for (int i = 0; i < solutionCount; i++) {
				generator.loadSolution(i);
				var pathWithIndex = CliUtils.getFileNameWithIndex(outputPath, i + 1);
				serializer.saveModel(generator, pathWithIndex, false);
			}
		}
		generator.close();
		return RefineryCli.EXIT_SUCCESS;
	}

	private void printHeaderToCsv(List<String> header, String csvPath) throws IOException {
		File csvOutputFile = new File(csvPath);
		try (FileWriter fw = new FileWriter(csvOutputFile, true)) {
			fw.write(String.join(",", header));
			fw.write("\n");
		}
	}


	private void printToCsv(RunConfiguration config, long runtime, String csvPath) throws IOException {
		File csvOutputFile = new File(csvPath);
		try (FileWriter fw = new FileWriter(csvOutputFile, true)) {
			fw.write(String.join(",", timestamp.toString(), config.measurementType().name(), config.input(),
					config.output(), String.valueOf(config.generateUp()), String.valueOf(config.timeout()),
					String.valueOf(runtime)));
			fw.write("\n");
		}
	}
}

