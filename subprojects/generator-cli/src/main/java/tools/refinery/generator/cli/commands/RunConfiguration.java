package tools.refinery.generator.cli.commands;

public record RunConfiguration(MeasurementType measurementType, boolean generateUp, String input,
							   String output, long timeout, int count) {
}
