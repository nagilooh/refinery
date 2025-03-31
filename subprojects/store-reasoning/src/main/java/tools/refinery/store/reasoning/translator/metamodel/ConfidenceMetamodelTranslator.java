/*
 * SPDX-FileCopyrightText: 2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.reasoning.translator.metamodel;

import tools.refinery.logic.dnf.FunctionalQuery;
import tools.refinery.logic.dnf.Query;
import tools.refinery.logic.term.Variable;
import tools.refinery.store.model.ModelStoreBuilder;
import tools.refinery.store.model.ModelStoreConfiguration;
import tools.refinery.store.query.ModelQueryBuilder;
import tools.refinery.store.reasoning.translator.containment.ContainerTypeInferenceTranslator;
import tools.refinery.store.reasoning.translator.containment.ContainmentHierarchyTranslator;
import tools.refinery.store.reasoning.translator.crossreference.DirectedCrossReferenceConfidenceTranslator;
import tools.refinery.store.reasoning.translator.crossreference.DirectedCrossReferenceTranslator;
import tools.refinery.store.reasoning.translator.crossreference.UndirectedCrossReferenceTranslator;
import tools.refinery.store.reasoning.translator.opposite.OppositeRelationTranslator;
import tools.refinery.store.reasoning.translator.typehierarchy.TypeHierarchyTranslator;

import java.util.ArrayList;
import java.util.List;

import static tools.refinery.logic.term.real.RealTerms.REAL_SUM;

public class ConfidenceMetamodelTranslator implements ModelStoreConfiguration {
	private final ConfidenceMetamodel metamodel;
	private final List<FunctionalQuery<Double>> upQueries = new ArrayList<>();
	private final List<FunctionalQuery<Double>> lowQueries = new ArrayList<>();
	private final List<FunctionalQuery<Double>> currentQueries = new ArrayList<>();

	private FunctionalQuery<Double> upQuery;
	private FunctionalQuery<Double> lowQuery;
	private FunctionalQuery<Double> currentQuery;

	public ConfidenceMetamodelTranslator(ConfidenceMetamodel metamodel) {
		this.metamodel = metamodel;
	}

	@Override
	public void apply(ModelStoreBuilder storeBuilder) {
		storeBuilder.with(new TypeHierarchyTranslator(metamodel.typeHierarchy()));
		storeBuilder.with(new ContainmentHierarchyTranslator(metamodel.containmentHierarchy()));
		storeBuilder.with(new ContainerTypeInferenceTranslator(metamodel.typeHierarchy(),
				metamodel.containmentHierarchy()));
		for (var entry : metamodel.directedCrossReferences().entrySet()) {
			storeBuilder.with(new DirectedCrossReferenceTranslator(entry.getKey(), entry.getValue()));
		}
		for (var entry : metamodel.directedConfidenceCrossReferences().entrySet()) {
			var translator = new DirectedCrossReferenceConfidenceTranslator(entry.getKey(), entry.getValue());
			upQueries.add(translator.getUpQuery());
			lowQueries.add(translator.getLowQuery());
			currentQueries.add(translator.getCurrentQuery());
			storeBuilder.with(translator);
		}

		var upOutputVariable = Variable.of("upOutput", Double.class);
		var upHelperBuilder = Query.builder().output(upOutputVariable);
		for(var query : upQueries) {
			upHelperBuilder.clause(upOutputVariable.assign(query.aggregate(REAL_SUM)));
		}
		var upHelper = upHelperBuilder.build();
		upQuery = Query.of("up#sum", Double.class, (builder, output) -> builder
				.clause(
						output.assign(upHelper.aggregate(REAL_SUM))
				));

		var downOutputVariable = Variable.of("downOutput", Double.class);
		var downHelperBuilder = Query.builder().output(downOutputVariable);
		for(var query : lowQueries) {
			downHelperBuilder.clause(downOutputVariable.assign(query.aggregate(REAL_SUM)));
		}
		var downHelper = downHelperBuilder.build();
		lowQuery = Query.of("down#sum", Double.class, (builder, output) -> builder
				.clause(
						output.assign(downHelper.aggregate(REAL_SUM))
				));

		var currentOutputVariable = Variable.of("currentOutput", Double.class);
		var currentHelperBuilder = Query.builder().output(currentOutputVariable);
		for(var query : currentQueries) {
			currentHelperBuilder.clause(currentOutputVariable.assign(query.aggregate(REAL_SUM)));
		}
		var currentHelper = currentHelperBuilder.build();
		currentQuery = Query.of("current#sum", Double.class, (builder, output) -> builder
				.clause(
						output.assign(currentHelper.aggregate(REAL_SUM))
				));

		var modelQueryBuilder = storeBuilder.getAdapter(ModelQueryBuilder.class);
		modelQueryBuilder.query(upQuery);
		modelQueryBuilder.query(lowQuery);
		modelQueryBuilder.query(currentQuery);

		for (var entry : metamodel.undirectedCrossReferences().entrySet()) {
			storeBuilder.with(new UndirectedCrossReferenceTranslator(entry.getKey(), entry.getValue()));
		}
		for (var entry : metamodel.oppositeReferences().entrySet()) {
			storeBuilder.with(new OppositeRelationTranslator(entry.getKey(), entry.getValue()));
		}
	}

	public FunctionalQuery<Double> getUpQuery() {
		return upQuery;
	}

	public FunctionalQuery<Double> getLowQuery() {
		return lowQuery;
	}

	public FunctionalQuery<Double> getCurrentQuery() {
		return currentQuery;
	}
}
