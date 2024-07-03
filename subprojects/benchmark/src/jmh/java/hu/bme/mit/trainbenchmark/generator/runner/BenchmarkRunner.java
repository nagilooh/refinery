package hu.bme.mit.trainbenchmark.generator.runner;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

import hu.bme.mit.trainbenchmark.constants.ModelConstants;
import hu.bme.mit.trainbenchmark.generator.DummySerializer;
import hu.bme.mit.trainbenchmark.generator.ModelSerializer;
import hu.bme.mit.trainbenchmark.generator.Scenario;
import hu.bme.mit.trainbenchmark.generator.dolt.DoltSerializer;
import hu.bme.mit.trainbenchmark.generator.dolt.DoltSerializerNoPersistence;
import hu.bme.mit.trainbenchmark.generator.emf.EMFSerializerByTransactions;
import hu.bme.mit.trainbenchmark.generator.emf.EmfSerializerByResourceCopy;
import hu.bme.mit.trainbenchmark.generator.emf.EmfSerializerNoVC;
import hu.bme.mit.trainbenchmark.generator.git.GitSerializer;
import hu.bme.mit.trainbenchmark.generator.refinery.RefinerySerializer;
import hu.bme.mit.trainbenchmark.generator.refinery.RefinerySerializerFunctional;
import hu.bme.mit.trainbenchmark.generator.scalable.ScalableModelGenerator;

public class BenchmarkRunner {
	private static String delim = "\t";

	public ModelSerializer run(BenchmarkConfig config) {
		if (config instanceof ModelBuildConfig building) {
			return runBuilding(building);
		} else if(config instanceof ReferenceBuildingConfig reference) {
			return runReferenceBuilding(reference);
		} else {
			throw new IllegalArgumentException("Unexpected value: " + config);
		}
	}

	public void printModelBuildHeader() {
		System.out.println("scenario"+
				delim+"run"+
				delim+"tool"+
				delim+"size"+
				delim+"width"+
				delim+"commitsDuringBuild"+
				delim+"#errorsInCommit"+
				delim+"#commits"+
				delim+"restoreAfter"+
				delim+"linearHistory"+
				delim+"build(ns)"+
				delim+"commit(ns)"+
				delim+"manipulation(ns)");
	}

	protected ModelSerializer getSerializer(Tool tool, BenchmarkConfig config) {
		switch (tool) {
		case RefineryR: {
			return new RefinerySerializer();
		}
		case RefineryF: {
			return new RefinerySerializerFunctional();
		}
		case EMFnoVC: {
			return new EmfSerializerNoVC();
		}
		case EMFState: {
			return new EmfSerializerByResourceCopy();
		}
		case EMFDelta: {
			if(config instanceof ModelBuildConfig mBC) {
				return new EMFSerializerByTransactions(mBC.isHistoryLinear);
			} else {
				throw new IllegalArgumentException("Unexpected value: " + tool + " config " + config);
			}
		}
		case Dummy: {
			return new DummySerializer();
		}
		case Dolt: {
			return new DoltSerializer();
		}
		case DoltNoP: {
			return new DoltSerializerNoPersistence();
		}
		case Git: {
			return new GitSerializer();
		}
		default:
			throw new IllegalArgumentException("Unexpected value: " + tool);
		}
	}

	public ModelSerializer runBuilding(ModelBuildConfig config) {
		ModelSerializer last = null;
		for(int i = 0; i<config.repeat; i++) {
			try {
				last = runBuildingOnce(i+"",config);
				clean();
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		return last;
	}
	public ModelSerializer runReferenceBuilding(ReferenceBuildingConfig config) {
		ModelSerializer lastSerializer = null;
		for(int i = 0; i<config.repeat; i++) {
			try {
				lastSerializer = runReferenceBuildingOnce(i+"", config);
				clean();
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
		return lastSerializer;
	}

	protected ModelSerializer runBuildingOnce(String id, ModelBuildConfig config) throws Exception {
		ModelSerializer serializer = getSerializer(config.tool, config);
		ScalableModelGenerator generator = new ScalableModelGenerator(serializer,config.size,config.width,Scenario.INJECT,config.commitAfterActionsDuringBuild);
		System.out.print(MeasurementScenario.Building+
				delim+id+
				delim+config.tool+
				delim+config.size+
				delim+config.width+
				delim+config.commitAfterActionsDuringBuild+
				delim+config.errorsInOneEditAction+
				delim+config.numberOfCommits+
				delim+config.restoreAfterCommits+
				delim+config.isHistoryLinear);
		long start = System.nanoTime();
		// Build
		generator.generateModel();
		long generated = System.nanoTime()-start;
		System.out.print(delim+generated);
		// Commit
		serializer.commit();
		long commited = System.nanoTime() - start;
		System.out.print(delim+commited);
		// Insert changes
		generator.shuffleErrors(0);
		List<Long> commits = new ArrayList<>(config.numberOfCommits);
		final Random random = new Random(0);

		for(int errorCommit = 0; errorCommit<config.numberOfCommits; errorCommit++) {
			final int from = errorCommit*config.errorsInOneEditAction;
			final int to = from + config.errorsInOneEditAction;
			generator.applyErrors(from, to);
			commits.add(serializer.commit());


			if(config.restoreAfterCommits >= 0 && errorCommit % config.restoreAfterCommits == 0) {
				long randomVersion = commits.get(random.nextInt(commits.size()));
				serializer.restore(randomVersion);
				if(config.isHistoryLinear) {
					commits.removeIf(x -> x>randomVersion);
				}
			}
		}
		long commitedErrors = System.nanoTime() - start;
		System.out.print(delim+commitedErrors);

		System.out.println();
		serializer.endTransaction();
		//((RefinerySerializer) serializer).print();
		return serializer;
	}

	protected ModelSerializer runReferenceBuildingOnce(String id, ReferenceBuildingConfig config) throws Exception {
		ModelSerializer serializer = getSerializer(config.tool, config);
		System.out.print(MeasurementScenario.ReferenceMultiplicity+delim+id+delim+config.tool+delim+config.size+delim+config.width);
		// build
		long start = System.nanoTime();
		int o = 0;
		serializer.initModel();
		Object[] regions = new Object[config.size];
		for(int i=0; i<config.size; i++) {
			regions[i] = serializer.createVertex(o++,ModelConstants.REGION,Map.of(),Map.of(),Map.of());
		}
		long regionsGenerated = System.nanoTime()-start;
		System.out.print(delim+regionsGenerated);
		for(int i=0; i<config.size; i++) {
			for(int j = 0; j<config.width; j++) {
				serializer.createVertex(o++, ModelConstants.SEGMENT, Map.of(), Map.of(), Map.of(ModelConstants.ELEMENTS,regions[j]));
			}
		}
		long segmentsAdded = System.nanoTime() - start - regionsGenerated;
		System.out.print(delim+segmentsAdded);

		// Commit
		serializer.commit();
		long commited = System.nanoTime() - start;
		System.out.print(delim+commited);

		// Get
		System.out.println();
		return serializer;
	}
	protected void clean() {
		System.gc();
		try {
			Thread.sleep(1000);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
}
