package tools.refinery.generator.cli.commands;

import tools.refinery.generator.GeneratorResult;

import java.sql.Timestamp;

public record MeasurementResult(String timestamp, RunConfiguration config, long parsingTime,
								long initializationTime, long generationTime, long explorationTime,
								GeneratorResult generatorResult) {
}
