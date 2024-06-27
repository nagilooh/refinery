package hu.bme.mit.trainbenchmark.generator.runner;

public abstract class BenchmarkConfig {
	Tool tool;
	int size;
	int width;
	int repeat;
	public BenchmarkConfig(Tool tool, int size, int width, int repeat) {
		super();
		this.tool = tool;
		this.size = size;
		this.width = width;
		this.repeat = repeat;
	}
}
