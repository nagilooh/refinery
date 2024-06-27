package hu.bme.mit.trainbenchmark.generator.runner;

import java.io.IOException;

public class GeneratorRunner2 {
	public static void main(String[] args) {
//		RefinerySerializer serializer = new RefinerySerializerFunctional();
//		//CSVSerializer serializer = new CSVSerializer();
//		//EmfSerializer serializer = new EmfSerializer();
//		ScalableModelGenerator generator = new ScalableModelGenerator(serializer,400,Scenario.BATCH);
//		waitKey();
//		System.out.println("Start");
//		long start = System.nanoTime();
//		try {
//			generator.generateModel();
//		} catch (Exception e) {
//			// TODO Auto-generated catch block
//			e.printStackTrace();
//		}
//		System.out.println(generator.getErrors().size());
//		System.out.println("Done");
//		System.out.println(((System.nanoTime()-start) / 1000000) + " ms");
//		//waitKey();
//		serializer.commit();
//		System.out.println("Commited");
//		System.out.println(((System.nanoTime()-start) / 1000000) + " ms");
//		while(generator.getErrors().size() > 0) {
//			System.out.println(generator.getErrors().size());
////			try {
////				generator.applyErrors(30);
////			} catch (IOException e) {
////				// TODO Auto-generated catch block
////				e.printStackTrace();
////			}
//			serializer.commit();
//		}
//		//waitKey();
//		serializer.print();
	}

	private static void waitKey() {
		System.out.println(" - Press key -");
		try {
			System.in.read();
			System.in.read();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		System.out.println(" - Continue -");
	}
}
