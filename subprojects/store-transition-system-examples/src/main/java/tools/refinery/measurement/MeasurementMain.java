package tools.refinery.measurement;

import tools.refinery.language.ProblemStandaloneSetup;

import java.io.IOException;

public class MeasurementMain {

    static void main(String[] args) throws IOException {
		var injector = new ProblemStandaloneSetup().createInjectorAndDoEMFRegistration();
		var measurement = injector.getInstance(TrainBenchmarkMeasurement.class);
		var loadingStartTime = System.currentTimeMillis();
		measurement.init(32, MeasurementScenario.SIMULATE, Abstraction.ABSTRACT, false);
		var loadingEndTime = System.currentTimeMillis();
		System.out.println("Model loading time: " + (loadingEndTime - loadingStartTime) + " ms");
		measurement.check();
		var runStartTime = System.nanoTime();
		measurement.transformAndRecheck(30);
		var runEndTime = System.nanoTime();
		System.out.println("Model run time: " + (runEndTime - runStartTime) + " ns");
		System.out.println("Average time per step: " + ((runEndTime - runStartTime) / 30.0) + " ns");
		System.out.println("Average time per step: " + ((runEndTime - runStartTime) / 30.0 / 1000000.0) + " ms");
		measurement.finish();
    }
}
