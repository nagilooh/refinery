package tools.refinery.generator.cli.commands;

public record RunConfiguration(String scope, String input, String output, long timeout, boolean generateUp,
							   int count) {
}
