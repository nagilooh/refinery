/*
 * SPDX-FileCopyrightText: 2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.reasoning.translator.metamodel;

import org.junit.jupiter.api.Test;
import tools.refinery.logic.term.cardinalityinterval.CardinalityIntervals;
import tools.refinery.store.reasoning.representation.ConfidencePartialRelation;
import tools.refinery.store.reasoning.representation.PartialRelation;
import tools.refinery.store.reasoning.translator.TranslationException;

import static org.junit.jupiter.api.Assertions.assertThrows;

class ConfidenceMetamodelBuilderTest {
	private final PartialRelation university = new PartialRelation("University", 1);
	private final PartialRelation course = new PartialRelation("Course", 1);
	private final ConfidencePartialRelation courses = new ConfidencePartialRelation("courses", 2);
	private final ConfidencePartialRelation location = new ConfidencePartialRelation("location", 2);

	@Test
	void missingOppositeTest() {
		var builder = ConfidenceMetamodel.builder()
				.type(university)
				.type(course)
				.directedReference(courses, referenceBuilder -> referenceBuilder
						.source(university)
						.target(course)
						.opposite(location))
				.directedReference(location, referenceBuilder -> referenceBuilder
						.source(course)
						.target(university));

		assertThrows(TranslationException.class, builder::build);
	}

	@Test
	void invalidOppositeTypeTest() {
		var builder = ConfidenceMetamodel.builder()
				.type(university)
				.type(course)
				.directedReference(courses, referenceBuilder -> referenceBuilder
						.source(university)
						.target(course)
						.opposite(location))
				.directedReference(location, referenceBuilder -> referenceBuilder
						.source(course)
						.target(course)
						.opposite(courses));

		assertThrows(TranslationException.class, builder::build);
	}

	@Test
	void invalidOppositeMultiplicityTest() {
		var invalidMultiplicity = new PartialRelation("invalidMultiplicity", 1);

		var builder = ConfidenceMetamodel.builder()
				.type(university)
				.type(course)
				.directedReference(courses, referenceBuilder -> referenceBuilder
						.containment(true)
						.source(university)
						.target(course)
						.opposite(location))
				.directedReference(location, referenceBuilder -> referenceBuilder
						.source(course)
						.multiplicity(CardinalityIntervals.atLeast(2), invalidMultiplicity)
						.target(university)
						.opposite(courses));

		assertThrows(TranslationException.class, builder::build);
	}
}
