package tools.refinery.measurement;

import tools.refinery.logic.dnf.RelationalQuery;
import tools.refinery.store.transition.system.statespace.Transition;

import java.io.IOException;
import java.util.List;
import java.util.Map;

public interface MeasurementCase {
	void init(int size, MeasurementScenario scenario, Abstraction abstraction, boolean visualize) throws IOException;
	void run(int steps);
	void finish();
	void check();
	void transformAndRecheck(int steps);

	Map<RelationalQuery, List<Long>> getQueryTimes();

	Map<RelationalQuery, List<Integer>> getQueryMatches();

	Map<Transition, List<Long>> getTransitionTimes();

	Map<Transition, List<Long>> getPropagationTimes();

	Map<Transition, List<Long>> getFlushTimes();
}
