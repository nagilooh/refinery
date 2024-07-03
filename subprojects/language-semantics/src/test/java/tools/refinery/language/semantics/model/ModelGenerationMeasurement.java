/*
 * SPDX-FileCopyrightText: 2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.language.semantics.model;

import com.google.inject.Inject;
import com.google.inject.Injector;
import org.eclipse.xtext.testing.InjectWith;
import org.eclipse.xtext.testing.extensions.InjectionExtension;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import tools.refinery.language.ProblemStandaloneSetup;
import tools.refinery.language.model.tests.utils.ProblemParseHelper;
import tools.refinery.language.semantics.ModelInitializer;
import tools.refinery.language.tests.ProblemInjectorProvider;
import tools.refinery.store.dse.propagation.PropagationAdapter;
import tools.refinery.store.dse.strategy.BestFirstStoreManager;
import tools.refinery.store.dse.transition.DesignSpaceExplorationAdapter;
import tools.refinery.store.model.ModelStore;
import tools.refinery.store.query.interpreter.QueryInterpreterAdapter;
import tools.refinery.store.reasoning.ReasoningAdapter;
import tools.refinery.store.reasoning.ReasoningStoreAdapter;
import tools.refinery.store.statecoding.StateCoderAdapter;
import tools.refinery.visualization.ModelVisualizerAdapter;
import tools.refinery.visualization.internal.FileFormat;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;

@ExtendWith(InjectionExtension.class)
@InjectWith(ProblemInjectorProvider.class)
@Disabled("For debugging purposes only")
class ModelGenerationMeasurement {
	@Inject
	private ProblemParseHelper parseHelper;

	@Inject
	private ModelInitializer modelInitializer;

	void socialNetworkTest(String scope) {
		socialNetworkTest(scope, 1);
	}

	void socialNetworkTest(String scope, int randomseed) {
		var parsedProblem = parseHelper.parse("""
				// Metamodel
				class Person {
				    contains Post posts opposite author
				    Person friend opposite friend
				}

				class Post {
				    container Person[0..1] author opposite posts
				    Post replyTo
				}

				// Constraints
				error replyToNotFriend(Post x, Post y) <->
				    replyTo(x, y),
				    author(x, xAuthor),
				    author(y, yAuthor),
				    xAuthor != yAuthor,
				    !friend(xAuthor, yAuthor).

				error replyToCycle(Post x) <-> replyTo+(x, x).

				// Scope
				scope node = %s.
				""".formatted(scope));
		assertThat(parsedProblem.getResourceErrors(), empty());
		var problem = parsedProblem.problem();

		var storeBuilder = ModelStore.builder()
				.with(QueryInterpreterAdapter.builder())
				.with(ModelVisualizerAdapter.builder()
						.withOutputPath("test_output")
						.withFormat(FileFormat.DOT)
						.withFormat(FileFormat.SVG)
//						.saveStates()
						.saveDesignSpace())
				.with(PropagationAdapter.builder())
				.with(StateCoderAdapter.builder())
				.with(DesignSpaceExplorationAdapter.builder())
				.with(ReasoningAdapter.builder());

		var modelSeed = modelInitializer.createModel(problem, storeBuilder);

		var store = storeBuilder.build();

		var initialModel = store.getAdapter(ReasoningStoreAdapter.class).createInitialModel(modelSeed);

		var initialVersion = initialModel.commit();

		var bestFirst = new BestFirstStoreManager(store, 1);
		bestFirst.startExploration(initialVersion, randomseed);
	}

	void statechartTest(String scope) {
		statechartTest(scope, 1);
	}

	void statechartTest(String scope, int randomseed) {
		var parsedProblem = parseHelper.parse("""
				// Metamodel
				abstract class CompositeElement {
				    contains Region[] regions
				}

				class Region {
				    contains Vertex[] vertices opposite region
				}

				abstract class Vertex {
				    container Region[0..1] region opposite vertices
				    contains Transition[] outgoingTransition opposite source
				    Transition[] incomingTransition opposite target
				}

				class Transition {
				    container Vertex[0..1] source opposite outgoingTransition
				    Vertex target opposite incomingTransition
				}

				abstract class Pseudostate extends Vertex.

				abstract class RegularState extends Vertex.

				class Entry extends Pseudostate.

				class Exit extends Pseudostate.

				class FinalState extends RegularState.

				class State extends RegularState, CompositeElement.

				class Statechart extends CompositeElement.

				// Constraints

				/////////
				// Entry
				/////////

				pred entryInRegion(Region r, Entry e) <->
					vertices(r, e).

				error noEntryInRegion(Region r) <->
				    !entryInRegion(r, _).

				error multipleEntryInRegion(Region r) <->
				    entryInRegion(r, e1),
				    entryInRegion(r, e2),
				    e1 != e2.

				error incomingToEntry(Transition t, Entry e) <->
				    target(t, e).

				error noOutgoingTransitionFromEntry(Entry e) <->
				    !source(_, e).

				error multipleTransitionFromEntry(Entry e, Transition t1, Transition t2) <->
				    outgoingTransition(e, t1),
				    outgoingTransition(e, t2),
				    t1 != t2.

				/////////
				// Exit
				/////////

				error outgoingFromExit(Transition t, Exit e) <->
				    source(t, e).

				/////////
				// Final
				/////////

				error outgoingFromFinal(Transition t, FinalState e) <->
				    source(t, e).

				/////////
				// State vs Region
				/////////

				pred stateInRegion(Region r, State s) <->
				    vertices(r, s).

				error noStateInRegion(Region r) <->
				    !stateInRegion(r, _).

				scope node = %s, Statechart = 1.
				""".formatted(scope));
		assertThat(parsedProblem.getResourceErrors(), empty());
		var problem = parsedProblem.problem();

		var storeBuilder = ModelStore.builder()
				.with(QueryInterpreterAdapter.builder())
//				.with(ModelVisualizerAdapter.builder()
//						.withOutputPath("test_output")
//						.withFormat(FileFormat.DOT)
//						.withFormat(FileFormat.SVG)
//						.saveStates()
//						.saveDesignSpace())
				.with(PropagationAdapter.builder())
				.with(StateCoderAdapter.builder())
				.with(DesignSpaceExplorationAdapter.builder())
				.with(ReasoningAdapter.builder());

		var modelSeed = modelInitializer.createModel(problem, storeBuilder);

		var store = storeBuilder.build();

		var initialModel = store.getAdapter(ReasoningStoreAdapter.class).createInitialModel(modelSeed);

		var initialVersion = initialModel.commit();

		var bestFirst = new BestFirstStoreManager(store, 1);
		bestFirst.startExploration(initialVersion, randomseed);
	}

	void filesystemTest(String scope) {
		filesystemTest(scope, 1);
	}

	void filesystemTest(String scope, int randomseed) {
		var parsedProblem = parseHelper.parse("""
				class Filesystem {
					contains Entry root
				}

				abstract class Entry.

				class Directory extends Entry {
					contains Entry[] entries
				}

				class File extends Entry.

				Filesystem(fs).

				scope Filesystem += 0, Entry = %s.
				""".formatted(scope));
		assertThat(parsedProblem.getResourceErrors(), empty());
		var problem = parsedProblem.problem();

		var storeBuilder = ModelStore.builder()
				.with(QueryInterpreterAdapter.builder())
//				.with(ModelVisualizerAdapter.builder()
//						.withOutputPath("test_output")
//						.withFormat(FileFormat.DOT)
//						.withFormat(FileFormat.SVG)
//						.saveStates()
//						.saveDesignSpace())
				.with(PropagationAdapter.builder())
				.with(StateCoderAdapter.builder())
				.with(DesignSpaceExplorationAdapter.builder())
				.with(ReasoningAdapter.builder());

		var modelSeed = modelInitializer.createModel(problem, storeBuilder);

		var store = storeBuilder.build();

		var initialModel = store.getAdapter(ReasoningStoreAdapter.class).createInitialModel(modelSeed);

		var initialVersion = initialModel.commit();

		var bestFirst = new BestFirstStoreManager(store, 1);
		bestFirst.startExploration(initialVersion, randomseed);
	}

	void famTest(String scope) {
		famTest(scope, 1);
	}

	// https://refinery.services/#/1/KLUv_WCQBT0QACbTOyRAiTgHZ_jKhU2kqxuo8pvUz1lqjHBbzfkCKY2jtgC9MdyljQstADEAMwAwEuHL6k_12g5yVbG7tpSe9jbrspfb_y-G9yqlk8GUy67bRuYQ5tTeLV_DSAb8pHesOkIU1jmSeixt09Gzs2DKx53rqKcJj0a02gv6Zn5_nEkjfuiL3bTdpL3GWg0rr1orsfTZNk7hgKXZ4s46QpQBAIEv7mdKtI33dK0G7sd5HIrbNtx_Fk7HahsoWceSjkwFicUZZwrF4oxrxHutuMEFIaiUeBMvD6AzRkp1IA6LRZObEEAKYhvmDFIOzJVmRoA5eaQBeqhBFkMjQpKmlGEOIEJCmJS2ARIgcyDIwUhoJSqRCqK4KShlDJEMDDEvAY6FNQrDNh0KQbcw0OEODwQftBxGZAiKs8psO4hQO5TWUkT-Jpug7MDJATOw8-0ns5b5DkU7JHcCcuqbnCqKwnx10WCuGp_UrvfD3iGeZl9gQgN-scDzceo5q8dvtTJqFUkjnLEZmCsMEXXgeI2D1RaIocCLueUJZgJXJsWAD2IvTBM7IppOxOL2yTs48Xn_wiGJfiNOBZFZy_gBbL2Sxurg10czrvAL8k2nfhR8uD_LkPdJVQjNpMvvxBFiePWANWrgIRpbIItZ-GFWj44awjG2UlzMiKYe6p4jpZFvA2DxF0Mginr3TR6lCg==
	void famTest(String scope, int randomseed) {
		var parsedProblem = parseHelper.parse("""
				abstract class FunctionalElement {
					  contains FunctionalInterface interface opposite element
					  container Function parent opposite subElements
				  }

				  class FunctionalArchitectureModel {
					  contains FunctionalElement[] rootElements
				  }

				  class Function extends FunctionalElement {
					  contains FunctionalElement[] subElements opposite parent
				  }

				  class FAMTerminator {
					  container FunctionalData data opposite terminator
				  }

				  class InformationLink {
					  container FunctionalOutput from opposite outgoingLinks
					  FunctionalInput[1] to opposite IncomingLinks
				  }

				  class FunctionalInterface {
					  contains FunctionalData[] data opposite interface
					  container FunctionalElement element opposite interface
				  }

				  class FunctionalInput extends FunctionalData {
					  InformationLink[] IncomingLinks opposite to
				  }

				  class FunctionalOutput extends FunctionalData {
					  contains InformationLink[] outgoingLinks opposite from
				  }

				  abstract class FunctionalData {
					  contains FAMTerminator terminator opposite data
					  container FunctionalInterface interface opposite data
				  }

				  error terminatorAndInformation(FAMTerminator t, InformationLink i) <->
					  outgoingLinks(Out, i),
					  terminator(Out, t);
					  to(i, In),
					  terminator(In, t).

				  pred hasRoot(Function F) <->
					rootElements(_Model, F).

				  pred hasInt(Function F) <->
					  !parent(_Child, F),
					!rootElements(_Model, F).

				  pred hasLeaf(Function Fnction) <->
					parent(F, _Par),
					parent(_Child, F).

				  error noRoot(FunctionalArchitectureModel fam) <->
					  !hasRoot(_).

				  error noInt(FunctionalArchitectureModel fam) <->
					  !hasInt(_).

				  error noLeaf(FunctionalArchitectureModel fam) <->
					  !hasLeaf(_).
				  scope node = %s, FunctionalArchitectureModel = 1.
				""".formatted(scope));
		assertThat(parsedProblem.getResourceErrors(), empty());
		var problem = parsedProblem.problem();

		var storeBuilder = ModelStore.builder()
				.with(QueryInterpreterAdapter.builder())
//				.with(ModelVisualizerAdapter.builder()
//						.withOutputPath("test_output")
//						.withFormat(FileFormat.DOT)
//						.withFormat(FileFormat.SVG)
//						.saveStates()
//						.saveDesignSpace())
				.with(PropagationAdapter.builder())
				.with(StateCoderAdapter.builder())
				.with(DesignSpaceExplorationAdapter.builder())
				.with(ReasoningAdapter.builder());

		var modelSeed = modelInitializer.createModel(problem, storeBuilder);

		var store = storeBuilder.build();

		var initialModel = store.getAdapter(ReasoningStoreAdapter.class).createInitialModel(modelSeed);

		var initialVersion = initialModel.commit();

		var bestFirst = new BestFirstStoreManager(store, 1);
		bestFirst.startExploration(initialVersion, randomseed);
	}

	void ecoreTest(String scope) {
		ecoreTest(scope, 1);
	}

	// https://refinery.services/#/1/KLUv_WDJCCUVALZUPyUQyzgHjuO9LLlXWsC2IZ90VFFehhGFQPX2ZsDM7I-BUVXVKIBxMgAzADQA69Zp72uZmewhe1-dcqhLT7LPtfbbZUxxtlr_oJStbha3xcko7VvzfFuR4-6GkzEq1QLTOkfkw8PrwoeW5Qk9fIdxjg2Lb3s-5tx76On2hmjiZHTxQfjOj5TTvpTQc3GvA1XyLlMVGME3jfO22nEhEOO5-HXTB6X8dpHKWkdbD_j9Slkpz0_q0JQivVKF7KbcMVLGNs3x9wgBisWCkQR56W0evhYZUyQ0AxQBUCAwkAlNRDs-w0IWAt-jkh_zMy_yzGilsiZuSGs9Az6EC7-eMAmAwqjxjoyMJAVJaVkDIEJilKo7DxLgoSTGsUjMSqKQyQwtLijUag5zSAHo3d3r9JUIK9sUopdMgkMONPF3eRFdxA-MdwWwkgPQDmyrYpeiz10OBXbfZ3YuWyS1r-Bf8Roa2gE_oI6VefkFtMAgr2ssCbbrJ2rvQ1ojkZ_TAtkLssOnjXCxwBjUiDcUtttMvlR0BY2esEV-OIWZVoifinhkYPMHzBDU-8MU6KENTF26cHoZvc1SmvxgcgCvLBBaMgRiRrOxgMRTIi7Jr3JR2-dG2n89nVUQdmxb4w4aT56PM-gAx4j2_7z-GL1Son_FN7h7j_CcDDkS52vq39MPwZm8EU8Kd8gRvtF28xmsYzb9xLAxY0jARqHXxYCG9HrbklZfWiwjEyfRoPGApMRVzPpw4EBFzKjoCkyRlmTP6cYWgmFzNpUJSHIxdgGRDKKMB76w-_jNe7DtE7TCXShYWo0fuHR7Mo2VxWIHZUCSsWMsTn0TBHXqmgbFvj9VSUYFSH-qo19Nxwpr04yEWVAa_IRUs-Rk1JWQA5EB_l68D6pKmsmRxas=
	void ecoreTest(String scope, int randomseed) {
		var parsedProblem = parseHelper.parse("""
				class EAttribute extends EStructuralFeature.

				class EAnnotation extends EModelElement {
				    contains EStringToStringMapEntry[] details
				    container EModelElement eModelElement opposite eAnnotations
				    contains EObject[] contents
				    EObject[] references
				}

				class EClass extends EClassifier {
				    EClass[] eSuperTypes
				    contains EOperation[] eOperations opposite eContainingClass
				    contains EStructuralFeature[] eStructuralFeatures opposite eContainingClass
				    contains EGenericType[] eGenericSuperTypes
				}

				abstract class EClassifier extends ENamedElement {
				    container EPackage ePackage opposite eClassifiers
				    contains ETypeParameter[] eTypeParameters
				}

				class EDataType extends EClassifier.

				class EEnum extends EDataType {
				    contains EEnumLiteral[] eLiterals opposite eEnum
				}

				class EEnumLiteral extends ENamedElement {
				    container EEnum eEnum opposite eLiterals
				}

				abstract class EModelElement {
				    contains EAnnotation[] eAnnotations opposite eModelElement
				}

				abstract class ENamedElement extends EModelElement.

				class EObject.

				class EOperation extends ETypedElement {
				    container EClass eContainingClass opposite eOperations
				    contains ETypeParameter[] eTypeParameters
				    contains EParameter[] eParameters opposite eOperation
				    EClassifier[] eExceptions
				    contains EGenericType[] eGenericExceptions
				}

				class EPackage extends ENamedElement {
				    contains EClassifier[] eClassifiers opposite ePackage
				    contains EPackage[] eSubpackages opposite eSuperPackage
				    container EPackage eSuperPackage opposite eSubpackages
				}

				class EParameter extends ETypedElement {
				    container EOperation eOperation opposite eParameters
				}

				class EReference extends EStructuralFeature {
				    EReference eOpposite
				    EAttribute[] eKeys
				}

				abstract class EStructuralFeature extends ETypedElement {
				    container EClass eContainingClass opposite eStructuralFeatures
				}

				abstract class ETypedElement extends ENamedElement.

				class EStringToStringMapEntry.

				class EGenericType {
				    contains EGenericType eUpperBound
				    contains EGenericType[] eTypeArguments
				    EClassifier[1] eRawType
				    contains EGenericType eLowerBound
				    ETypeParameter eTypeParameter
				    EClassifier eClassifier
				}

				class ETypeParameter extends ENamedElement {
				    contains EGenericType[] eBounds
				}

				class Model {
				    contains EPackage[1..*] packages
				}

				error loopInInheritance(EClass a) <->
				    eSuperTypes+(a, a).

				scope node = %s, Model = 1.
				""".formatted(scope));
		assertThat(parsedProblem.getResourceErrors(), empty());
		var problem = parsedProblem.problem();

		var storeBuilder = ModelStore.builder()
				.with(QueryInterpreterAdapter.builder())
//				.with(ModelVisualizerAdapter.builder()
//						.withOutputPath("test_output")
//						.withFormat(FileFormat.DOT)
//						.withFormat(FileFormat.SVG)
//						.saveStates()
//						.saveDesignSpace())
				.with(PropagationAdapter.builder())
				.with(StateCoderAdapter.builder())
				.with(DesignSpaceExplorationAdapter.builder())
				.with(ReasoningAdapter.builder());

		var modelSeed = modelInitializer.createModel(problem, storeBuilder);

		var store = storeBuilder.build();

		var initialModel = store.getAdapter(ReasoningStoreAdapter.class).createInitialModel(modelSeed);

		var initialVersion = initialModel.commit();

		var bestFirst = new BestFirstStoreManager(store, 1);
		bestFirst.startExploration(initialVersion, randomseed);
	}

	private static final int TIMEOUT = 60000;
//	private static final String PROBLEM = "ecoreTest";

	public static void main(String[] args) {
		ProblemStandaloneSetup.doSetup();
		var injector = new ProblemStandaloneSetup().createInjectorAndDoEMFRegistration();
		List<String[]> warmupResults = new ArrayList<>();
		warmupResults.add(new String[]
				{ "testCase", "size", "startTime",
						"endTime", "runtime" });
		List<String[]> results = new ArrayList<>();
		results.add(new String[]
				{ "testCase", "size", "startTime",
						"endTime", "runtime" });

		try {
			// warmup
			for (int i = 100; i <= 500; i += 100) {
				System.out.println("Size " + i);
				// measurement
				for (int j = 0; j < 5; j++) {
					System.gc();
					var test = injector.getInstance(ModelGenerationMeasurement.class);
//					var scope = i + ".." + (int) (1.1 * i);
					var scope = String.valueOf(i);

					long startTime = System.currentTimeMillis();
					test.ecoreTest(scope);										// <-- Select the test case here
					long endTime = System.currentTimeMillis();
					long runtime = endTime - startTime;
					warmupResults.add(new String[]
							{"warmup", String.valueOf(i), String.valueOf(j), String.valueOf(startTime),
									String.valueOf(endTime), String.valueOf(runtime) });
				}
			}

			// measurement
			results.addAll(statechartMeasurement(injector));
			results.addAll(ecoreMeasurement(injector));
			results.addAll(famMeasurement(injector));
			results.addAll(filesystemMeasurement(injector));
			results.addAll(socialNetworkMeasurement(injector));

		} catch (Throwable e) {
			e.printStackTrace();
		}
		File csvOutputFile = new File("output/results-all-60sec.csv");
		try (PrintWriter pw = new PrintWriter(csvOutputFile)) {
			for (String[] result : results) {
				String s = convertToCSV(result);
				pw.println(s);
			}
		} catch (FileNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

	private static List<String[]> socialNetworkMeasurement(Injector injector) {
		List<String[]> results = new ArrayList<>();
		var success = true;
		int i = 190;
		while (success) {
			System.out.println("Size " + i);
			var timedOut = 0;
			success = false;
			while (!success && timedOut < 5) {
				System.gc();
				var test = injector.getInstance(ModelGenerationMeasurement.class);
				var scope = String.valueOf(i);

				long startTime = System.currentTimeMillis();
				test.socialNetworkTest(scope, timedOut + 1);										// <-- Select the test case here
				long endTime = System.currentTimeMillis();
				long runtime = endTime - startTime;
				System.out.println("Size " + i + " took " + runtime + "ms");
				if (runtime > TIMEOUT) {
					timedOut++;
				}
				else {
					success = true;
					results.add(new String[]
							{"socialNetworkTest", String.valueOf(i), String.valueOf(startTime),
									String.valueOf(endTime), String.valueOf(runtime) });
				}
			}
			i += 10;
		}
		return results;
	}

	private static List<String[]> famMeasurement(Injector injector) {
		List<String[]> results = new ArrayList<>();
		var success = true;
		int i = 8500;
		while (success) {
			System.out.println("Size " + i);
			var timedOut = 0;
			success = false;
			while (!success && timedOut < 5) {
				System.gc();
				var test = injector.getInstance(ModelGenerationMeasurement.class);
				var scope = String.valueOf(i);

				long startTime = System.currentTimeMillis();
				test.famTest(scope, timedOut + 1);										// <-- Select the test case here
				long endTime = System.currentTimeMillis();
				long runtime = endTime - startTime;
				System.out.println("Size " + i + " took " + runtime + "ms");
				if (runtime > TIMEOUT) {
					timedOut++;
				}
				else {
					success = true;
					results.add(new String[]
							{"famTest", String.valueOf(i), String.valueOf(startTime),
									String.valueOf(endTime), String.valueOf(runtime) });
				}
			}
			i += 250;
		}
		return results;
	}

	private static List<String[]> filesystemMeasurement(Injector injector) {
		List<String[]> results = new ArrayList<>();
		var success = true;
		int i = 10500;
		while (success) {
			System.out.println("Size " + i);
			var timedOut = 0;
			success = false;
			while (!success && timedOut < 5) {
				System.gc();
				var test = injector.getInstance(ModelGenerationMeasurement.class);
				var scope = String.valueOf(i);

				long startTime = System.currentTimeMillis();
				test.filesystemTest(scope, timedOut + 1);										// <-- Select the test case here
				long endTime = System.currentTimeMillis();
				long runtime = endTime - startTime;
				System.out.println("Size " + i + " took " + runtime + "ms");
				if (runtime > TIMEOUT) {
					timedOut++;
				}
				else {
					success = true;
					results.add(new String[]
							{"filesystemTest", String.valueOf(i), String.valueOf(startTime),
									String.valueOf(endTime), String.valueOf(runtime) });
				}
			}
			i += 250;
		}
		return results;
	}

	private static List<String[]> statechartMeasurement(Injector injector) {
		List<String[]> results = new ArrayList<>();
		var success = true;
		int i = 2250;
		while (success) {
			System.out.println("Size " + i);
			var timedOut = 0;
			success = false;
			while (!success && timedOut < 5) {
				System.gc();
				var test = injector.getInstance(ModelGenerationMeasurement.class);
				var scope = String.valueOf(i);

				long startTime = System.currentTimeMillis();
				test.statechartTest(scope, timedOut + 1);										// <-- Select the test case here
				long endTime = System.currentTimeMillis();
				long runtime = endTime - startTime;
				System.out.println("Size " + i + " took " + runtime + "ms");
				if (runtime > TIMEOUT) {
					timedOut++;
				}
				else {
					success = true;
					results.add(new String[]
							{"statechartTest", String.valueOf(i), String.valueOf(startTime),
									String.valueOf(endTime), String.valueOf(runtime) });
				}
			}
			i += 250;
		}
		return results;
	}

	private static List<String[]> ecoreMeasurement(Injector injector) {
		List<String[]> results = new ArrayList<>();
		var success = true;
		int i = 1500;
		while (success) {
			System.out.println("Size " + i);
			var timedOut = 0;
			success = false;
			while (!success && timedOut < 5) {
				System.gc();
				var test = injector.getInstance(ModelGenerationMeasurement.class);
				var scope = String.valueOf(i);

				long startTime = System.currentTimeMillis();
				test.ecoreTest(scope, timedOut + 1);										// <-- Select the test case here
				long endTime = System.currentTimeMillis();
				long runtime = endTime - startTime;
				System.out.println("Size " + i + " took " + runtime + "ms");
				if (runtime > TIMEOUT) {
					timedOut++;
				}
				else {
					success = true;
					results.add(new String[]
							{"ecoreTest", String.valueOf(i), String.valueOf(startTime),
									String.valueOf(endTime), String.valueOf(runtime) });
				}
			}
			i += 250;
		}
		return results;
	}

	public static String convertToCSV(String[] data) {
		return Stream.of(data)
				.map(ModelGenerationMeasurement::escapeSpecialCharacters)
				.collect(Collectors.joining(","));
	}

	public static String escapeSpecialCharacters(String data) {
		String escapedData = data.replaceAll("\\R", " ");
		if (data.contains(",") || data.contains("\"") || data.contains("'")) {
			data = data.replace("\"", "\"\"");
			escapedData = "\"" + data + "\"";
		}
		return escapedData;
	}
}
