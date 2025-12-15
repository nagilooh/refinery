package tools.refinery.generator.cli.commands;

import java.sql.Timestamp;

public record MeasurementResult(String timestamp, RunConfiguration config, long parsingTime,
								long initializationTime, long generationTime, long explorationTime) {
}
