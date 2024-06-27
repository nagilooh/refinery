package hu.bme.mit.trainbenchmark.generator.runner;

public class ModelBuildConfig extends BenchmarkConfig {
	int commitAfterActionsDuringBuild;
	
	int errorsInOneEditAction;
	int numberOfCommits;
	
	int restoreAfterCommits;
	boolean isHistoryLinear;
	
	public ModelBuildConfig(Tool tool, int size, int width, int repeat, int commitAfterActionsDuringBuild,
			int errorsInOneEditAction, int numberOfCommits, int restoreAfterCommits, boolean isHistoryLinear) {
		super(tool, size, width, repeat);
		this.commitAfterActionsDuringBuild = commitAfterActionsDuringBuild;
		this.errorsInOneEditAction = errorsInOneEditAction;
		this.numberOfCommits = numberOfCommits;
		this.restoreAfterCommits = restoreAfterCommits;
		this.isHistoryLinear = isHistoryLinear;
	}
}
