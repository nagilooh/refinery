package tools.refinery.measurement;

import tools.refinery.language.ProblemStandaloneSetup;

public class MeasurementMain {

    static void main(String[] args) {
		var injector = new ProblemStandaloneSetup().createInjectorAndDoEMFRegistration();
		var measurement = injector.getInstance(TrainBenchmarkMeasurement.class);
		var loadingStartTime = System.currentTimeMillis();
		measurement.init(1, MeasurementScenario.SIMULATE, false);
		var loadingEndTime = System.currentTimeMillis();
		System.out.println("Model loading time: " + (loadingEndTime - loadingStartTime) + " ms");
		measurement.run(10);
		measurement.finish();
    }
}
