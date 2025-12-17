package tools.refinery.generator.cli.commands;

public record RunConfiguration(String name, int size, String input, String output, long timeout, boolean generateUp,
							   int count) {
}
