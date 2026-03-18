package tools.refinery.generator.cli.commands;

public record CountResult(long parseTime, long initTime, int allCount, int unknownCount) {
}
