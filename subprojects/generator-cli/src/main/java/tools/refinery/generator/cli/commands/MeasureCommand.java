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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Parameters(commandDescription = "Measure the generation of a model from a partial model")
public class MeasureCommand implements Command {
	private final CliProblemLoader loader;
	private final ModelGeneratorFactory generatorFactory;

	private String inputPath;
	private List<String> scopes = new ArrayList<>();
	private List<String> overrideScopes = new ArrayList<>();
	private int count = 1;
	private boolean generateUP = false;
	private long timeout = 10L;

	@Inject
	public MeasureCommand(CliProblemLoader loader, ModelGeneratorFactory generatorFactory,
                          CliProblemSerializer serializer) {
		this.loader = loader;
		this.generatorFactory = generatorFactory;
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
		var problem = loader.loadProblem(inputPath, scopes, overrideScopes);

		var start = System.currentTimeMillis();
		var generator = generatorFactory.createGenerator(problem, generateUP);
		var end = System.currentTimeMillis();
		System.out.println("Initialization time: " + (end - start));
		generator.setMaxNumberOfSolutions(1);
		for (int i = 0; i < count; i++) {
			System.out.println(i);
			generator.tryGenerateWithTimeout(timeout, TimeUnit.SECONDS);
			System.out.println(generator.getSolutionCount());
			System.out.println(generator.getGenerationTimes());
		}
		generator.close();
		return RefineryCli.EXIT_SUCCESS;
	}
}

