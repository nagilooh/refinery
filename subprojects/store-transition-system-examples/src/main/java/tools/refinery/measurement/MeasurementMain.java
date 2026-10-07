package tools.refinery.measurement;

import tools.refinery.language.ProblemStandaloneSetup;

import java.io.IOException;

public class MeasurementMain {

    static void main(String[] args) throws IOException {
//		for (int i = 0; i < 1; i++) {
//			var measurement = new VehicleMovingMeasurement();
//			var startTime = System.currentTimeMillis();
//			measurement.init(5);
//			var modelLoadingTime = System.currentTimeMillis() - startTime;
//			System.out.println("Model loading time: " + modelLoadingTime + " ms");
//        	measurement.run(500);
////			measurement.runTransitionSystem();
//			var transitionTime = System.currentTimeMillis() - startTime - modelLoadingTime;
//			System.out.println("Transition time: " + transitionTime + " ms");
//			measurement.finish();
//			measurement = null;
//			System.gc();
////			try {
////				Thread.sleep(1000);
////			} catch (InterruptedException e) {
////				throw new RuntimeException(e);
////			}
//		}


		ProblemStandaloneSetup.doSetup();
		var injector = new ProblemStandaloneSetup().createInjectorAndDoEMFRegistration();
		var measurement = injector.getInstance(DiningPhilosophersMeasurement.class);
		var startTime = System.currentTimeMillis();
		measurement.init(3);
		var modelLoadingTime = System.currentTimeMillis() - startTime;
		System.out.println("Model loading time: " + modelLoadingTime + " ms");
//		measurement.run(500);
		measurement.runTransitionSystem();
		var transitionTime = System.currentTimeMillis() - startTime - modelLoadingTime;
		System.out.println("Transition time: " + transitionTime + " ms");
		measurement.finish();


    }
}
