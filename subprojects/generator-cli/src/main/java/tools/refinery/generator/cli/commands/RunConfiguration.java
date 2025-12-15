package tools.refinery.generator.cli.commands;

public record RunConfiguration(String name, int size, String input, long timeout, boolean generateUp, int count) {
}
