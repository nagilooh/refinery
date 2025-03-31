/*
 * SPDX-FileCopyrightText: 2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.reasoning.translator.confidence;

import org.junit.jupiter.api.Test;
import tools.refinery.logic.term.cardinalityinterval.CardinalityIntervals;
import tools.refinery.logic.term.truthvalue.TruthValue;
import tools.refinery.logic.term.truthvalue.TruthValueConfidence;
import tools.refinery.store.dse.propagation.PropagationAdapter;
import tools.refinery.store.model.Model;
import tools.refinery.store.model.ModelStore;
import tools.refinery.store.query.ModelQueryAdapter;
import tools.refinery.store.query.interpreter.QueryInterpreterAdapter;
import tools.refinery.store.reasoning.ReasoningAdapter;
import tools.refinery.store.reasoning.ReasoningStoreAdapter;
import tools.refinery.store.reasoning.literal.Concreteness;
import tools.refinery.store.reasoning.refinement.ConcreteRelationConfidenceRefiner;
import tools.refinery.store.reasoning.representation.ConfidencePartialRelation;
import tools.refinery.store.reasoning.representation.PartialRelation;
import tools.refinery.store.reasoning.seed.ModelSeed;
import tools.refinery.store.reasoning.translator.containment.ContainmentHierarchyTranslator;
import tools.refinery.store.reasoning.translator.metamodel.ConfidenceMetamodel;
import tools.refinery.store.reasoning.translator.metamodel.ConfidenceMetamodelTranslator;
import tools.refinery.store.reasoning.translator.multiobject.MultiObjectTranslator;
import tools.refinery.store.tuple.Tuple;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.closeTo;

class ConfidenceMetamodelTest {
	private final PartialRelation person = new PartialRelation("Person", 1);
	private final PartialRelation student = new PartialRelation("Student", 1);
	private final PartialRelation teacher = new PartialRelation("Teacher", 1);
	private final PartialRelation university = new PartialRelation("University", 1);
	private final PartialRelation course = new PartialRelation("Course", 1);
	private final PartialRelation courses = new PartialRelation("courses", 2);
	private final PartialRelation location = new PartialRelation("location", 2);
	private final PartialRelation lecturer = new PartialRelation("lecturer", 2);
	private final PartialRelation invalidLecturerCount = new PartialRelation("invalidLecturerCount", 1);
	private final ConfidencePartialRelation enrolledStudentsConfidence = new ConfidencePartialRelation(
			"enrolledStudentsConfidence",	2);
	private final PartialRelation enrolledStudents = new PartialRelation("enrolledStudents", 2);
	private final PartialRelation invalidStudentCount = new PartialRelation("invalidStudentCount", 1);

	private static final double PRECISION = 0.00001;

	@Test
	void metamodelTest() {
		var metamodel = ConfidenceMetamodel.builder()
				.type(person, true)
				.type(student, person)
				.type(teacher, person)
				.type(university)
				.type(course)
				.reference(courses, builder -> builder
						.containment(true)
						.source(university)
						.target(course)
						.opposite(location))
				.reference(location, builder -> builder
						.source(course)
						.target(university)
						.opposite(courses))
				.reference(lecturer, builder -> builder
						.source(course)
						.multiplicity(CardinalityIntervals.ONE, invalidLecturerCount)
						.target(teacher))
				.directedReference(enrolledStudentsConfidence, builder -> builder
						.source(course)
						.multiplicity(CardinalityIntervals.SOME, invalidStudentCount)
						.target(student)
						.partialSymbol(enrolledStudents))
				.build();

		var seed = ModelSeed.builder(6)
				.seed(MultiObjectTranslator.COUNT_SYMBOL, builder -> builder
						.reducedValue(CardinalityIntervals.ONE)
						.put(Tuple.of(1), CardinalityIntervals.SET)
						.put(Tuple.of(4), CardinalityIntervals.SET))
				.seed(ContainmentHierarchyTranslator.CONTAINER_SYMBOL, builder -> builder
						.reducedValue(TruthValue.UNKNOWN))
				.seed(ContainmentHierarchyTranslator.CONTAINED_SYMBOL, builder -> builder
						.reducedValue(TruthValue.UNKNOWN))
				.seed(ContainmentHierarchyTranslator.CONTAINS_SYMBOL, builder -> builder
						.reducedValue(TruthValue.UNKNOWN))
				.seed(person, builder -> builder.reducedValue(TruthValue.UNKNOWN))
				.seed(student, builder -> builder.reducedValue(TruthValue.UNKNOWN))
				.seed(teacher, builder -> builder.reducedValue(TruthValue.UNKNOWN))
				.seed(university, builder -> builder
						.reducedValue(TruthValue.UNKNOWN)
						.put(Tuple.of(0), TruthValue.TRUE))
				.seed(course, builder -> builder
						.reducedValue(TruthValue.UNKNOWN)
						.put(Tuple.of(2), TruthValue.TRUE))
				.seed(courses, builder -> builder.reducedValue(TruthValue.UNKNOWN))
				.seed(location, builder -> builder
						.reducedValue(TruthValue.UNKNOWN)
						.put(Tuple.of(1, 0), TruthValue.TRUE))
				.seed(lecturer, builder -> builder
						.reducedValue(TruthValue.FALSE)
						.put(Tuple.of(1, 3), TruthValue.TRUE))
				.seed(enrolledStudentsConfidence, builder -> builder
						.reducedValue(new TruthValueConfidence(TruthValue.FALSE, 0.0))
						.put(Tuple.of(1, 4), new TruthValueConfidence(TruthValue.UNKNOWN, 0.7))
						.put(Tuple.of(1, 5), new TruthValueConfidence(TruthValue.TRUE, 1.0)))
				.build();

		var translator = new ConfidenceMetamodelTranslator(metamodel);

		try (var model = createModel(translator, seed)) {
			var reasoningAdapter = model.getAdapter(ReasoningAdapter.class);

			var coursesInterpretation = reasoningAdapter.getPartialInterpretation(Concreteness.PARTIAL, courses);
			assertThat(coursesInterpretation.get(Tuple.of(0, 1)), is(TruthValue.TRUE));
			assertThat(coursesInterpretation.get(Tuple.of(0, 2)), is(TruthValue.UNKNOWN));
			assertThat(coursesInterpretation.get(Tuple.of(0, 3)), is(TruthValue.FALSE));

			var invalidLecturerCountInterpretation = reasoningAdapter.getPartialInterpretation(Concreteness.PARTIAL,
					invalidLecturerCount);
			assertThat(invalidLecturerCountInterpretation.get(Tuple.of(1)), is(TruthValue.FALSE));
			assertThat(invalidLecturerCountInterpretation.get(Tuple.of(2)), is(TruthValue.ERROR));

			var enrolledStudentsInterpretation = reasoningAdapter.getPartialInterpretation(Concreteness.PARTIAL,
					enrolledStudents);
			assertThat(enrolledStudentsInterpretation.get(Tuple.of(1, 3)), is(TruthValue.FALSE));
			assertThat(enrolledStudentsInterpretation.get(Tuple.of(1, 4)), is(TruthValue.UNKNOWN));
			assertThat(enrolledStudentsInterpretation.get(Tuple.of(1, 5)), is(TruthValue.TRUE));

			var interpretation = reasoningAdapter.getPartialInterpretation(Concreteness.PARTIAL,
					enrolledStudentsConfidence);
			assertThat(interpretation.get(Tuple.of(1, 3)), is(TruthValueConfidence.FALSE));
			assertThat(interpretation.get(Tuple.of(1, 4)), is(new TruthValueConfidence(TruthValue.UNKNOWN, 0.7)));
			assertThat(interpretation.get(Tuple.of(1, 5)), is(TruthValueConfidence.TRUE));

			var candidateInterpretation = reasoningAdapter.getPartialInterpretation(Concreteness.CANDIDATE,
					enrolledStudentsConfidence);
			assertThat(candidateInterpretation.get(Tuple.of(1, 3)), is(TruthValueConfidence.FALSE));
			assertThat(candidateInterpretation.get(Tuple.of(1, 4)), is(TruthValueConfidence.FALSE));
			assertThat(candidateInterpretation.get(Tuple.of(1, 5)), is(new TruthValueConfidence(TruthValue.TRUE, 1.0)));

			assertThat(ConcreteRelationConfidenceRefiner.getConfidenceCost(), closeTo(0.0, PRECISION));

			var refiner = reasoningAdapter.getRefiner(enrolledStudents);
			refiner.merge(Tuple.of(1, 4), TruthValue.TRUE);

			assertThat(ConcreteRelationConfidenceRefiner.getConfidenceCost(), closeTo(Math.log(0.3), PRECISION));

			var queryEngine = model.getAdapter(ModelQueryAdapter.class);
			queryEngine.flushChanges();

			assertThat(interpretation.get(Tuple.of(1, 4)), is(TruthValueConfidence.TRUE));
			assertThat(enrolledStudentsInterpretation.get(Tuple.of(1, 4)), is(TruthValue.TRUE));
			assertThat(candidateInterpretation.get(Tuple.of(1, 4)), is(TruthValueConfidence.TRUE));


			refiner.merge(Tuple.of(1, 4), TruthValue.FALSE);
			queryEngine.flushChanges();

			assertThat(ConcreteRelationConfidenceRefiner.getConfidenceCost(), is(Double.NaN));


			assertThat(interpretation.get(Tuple.of(1, 4)), is(TruthValueConfidence.ERROR));
			assertThat(enrolledStudentsInterpretation.get(Tuple.of(1, 4)), is(TruthValue.ERROR));
			assertThat(candidateInterpretation.get(Tuple.of(1, 4)), is(TruthValueConfidence.ERROR));
		}
	}

	@Test
	void simpleContainmentTest() {
		var metamodel = ConfidenceMetamodel.builder()
				.type(university)
				.type(course)
				.reference(courses, builder -> builder
						.containment(true)
						.source(university)
						.target(course))
				.build();

		var seed = ModelSeed.builder(4)
				.seed(MultiObjectTranslator.COUNT_SYMBOL, builder -> builder
						.reducedValue(CardinalityIntervals.ONE)
						.put(Tuple.of(0), CardinalityIntervals.SET)
						.put(Tuple.of(1), CardinalityIntervals.SET))
				.seed(ContainmentHierarchyTranslator.CONTAINER_SYMBOL, builder -> builder
						.reducedValue(TruthValue.UNKNOWN))
				.seed(ContainmentHierarchyTranslator.CONTAINED_SYMBOL, builder -> builder
						.reducedValue(TruthValue.UNKNOWN))
				.seed(ContainmentHierarchyTranslator.CONTAINS_SYMBOL, builder -> builder
						.reducedValue(TruthValue.UNKNOWN))
				.seed(university, builder -> builder
						.reducedValue(TruthValue.UNKNOWN)
						.put(Tuple.of(0), TruthValue.TRUE))
				.seed(course, builder -> builder
						.reducedValue(TruthValue.UNKNOWN)
						.put(Tuple.of(1), TruthValue.TRUE))
				.seed(courses, builder -> builder
						.reducedValue(TruthValue.UNKNOWN)
						.put(Tuple.of(2, 3), TruthValue.TRUE))
				.build();

		var translator = new ConfidenceMetamodelTranslator(metamodel);

		try (var model = createModel(translator, seed)) {
			var coursesInterpretation = model.getAdapter(ReasoningAdapter.class)
					.getPartialInterpretation(Concreteness.PARTIAL, courses);

			assertThat(coursesInterpretation.get(Tuple.of(0, 1)), is(TruthValue.UNKNOWN));
			assertThat(coursesInterpretation.get(Tuple.of(0, 3)), is(TruthValue.FALSE));
			assertThat(coursesInterpretation.get(Tuple.of(2, 1)), is(TruthValue.UNKNOWN));
			assertThat(coursesInterpretation.get(Tuple.of(2, 3)), is(TruthValue.TRUE));
		}
	}

	private static Model createModel(ConfidenceMetamodelTranslator metamodelTranslator, ModelSeed seed) {
		var store = ModelStore.builder()
				.with(QueryInterpreterAdapter.builder())
				.with(PropagationAdapter.builder())
				.with(ReasoningAdapter.builder())
				.with(new MultiObjectTranslator())
				.with(metamodelTranslator)
				.build();

		return store.getAdapter(ReasoningStoreAdapter.class).createInitialModel(seed);
	}
}
