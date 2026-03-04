package tools.refinery.measurement;

public record CountResult(long parseTime, long initTime, int allCount, int unknownCount) {
}
