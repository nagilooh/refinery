package tools.refinery.generator.cli.commands;

import tools.refinery.generator.GeneratorResult;

public record MeasurementResult(String timestamp, RunConfiguration config, long parsingTime,
								long initializationTime, long generationTime, long explorationTime, int stateSpaceSize,
								int unknownCount, GeneratorResult generatorResult) {
}
