package tools.refinery.measurement;

import tools.refinery.language.ProblemStandaloneSetup;

import java.io.IOException;

public class MeasurementMain {

    static void main(String[] args) throws IOException {


//		ProblemStandaloneSetup.doSetup();
//		var injector = new ProblemStandaloneSetup().createInjectorAndDoEMFRegistration();
//		var measurement = injector.getInstance(VehicleMovingTextMeasurement.class);
//		var startTime = System.currentTimeMillis();
//		measurement.init(3);
//		var modelLoadingTime = System.currentTimeMillis() - startTime;
//		System.out.println("Model loading time: " + modelLoadingTime + " ms");
////		measurement.run(500);
//		measurement.runTransitionSystem();
//		var transitionTime = System.currentTimeMillis() - startTime - modelLoadingTime;
//		System.out.println("Transition time: " + transitionTime + " ms");
//		measurement.finish();



//		var vehicleMovingMeasurement = new VehicleMovingMeasurement();
//		vehicleMovingMeasurement.init(3, true);
//		vehicleMovingMeasurement.run(30);
////		vehicleMovingMeasurement.runTransitionSystem();
//		vehicleMovingMeasurement.finish();
//
		var injector = new ProblemStandaloneSetup().createInjectorAndDoEMFRegistration();
//		var diningPhilosophersMeasurement = injector.getInstance(DiningPhilosophersTextMeasurement.class);
//		diningPhilosophersMeasurement.init(3, true);
//		diningPhilosophersMeasurement.run(30);
////		diningPhilosophersMeasurement.runTransitionSystem();
//		diningPhilosophersMeasurement.finish();

		var statechartMeasurement = injector.getInstance(TrainBenchmarkTextMeasurement.class);
		statechartMeasurement.init(2, true);
		statechartMeasurement.run(15);
//		statechartMeasurement.runTransitionSystem();
		statechartMeasurement.finish();

    }
}
