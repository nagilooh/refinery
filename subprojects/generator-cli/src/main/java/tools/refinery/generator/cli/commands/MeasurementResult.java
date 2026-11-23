package tools.refinery.generator.cli.commands;

import tools.refinery.generator.GeneratorResult;
import tools.refinery.logic.dnf.RelationalQuery;
import tools.refinery.store.transition.system.statespace.Transition;

import java.util.List;
import java.util.Map;

public record MeasurementResult(String timestamp,
								RunConfiguration config,
								long initializationTime,
								Map<RelationalQuery, List<Long>> queryTimes,
								Map<RelationalQuery, List<Integer>> queryMatches,
								Map<Transition, List<Long>> transitionTimes,
								Map<Transition, List<Long>> propagationTimes,
								Map<Transition, List<Long>> flushTimes) {
}
