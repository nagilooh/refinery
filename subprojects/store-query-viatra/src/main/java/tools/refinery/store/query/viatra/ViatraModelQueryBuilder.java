package tools.refinery.store.query.viatra;

import org.eclipse.viatra.query.runtime.api.ViatraQueryEngineOptions;
import org.eclipse.viatra.query.runtime.matchers.backend.IQueryBackendFactory;
import org.eclipse.viatra.query.runtime.matchers.backend.QueryEvaluationHint;
import tools.refinery.store.model.ModelStore;
import tools.refinery.store.query.Dnf;
import tools.refinery.store.query.ModelQueryBuilder;

import java.util.Collection;
import java.util.function.Function;

@SuppressWarnings("UnusedReturnValue")
public interface ViatraModelQueryBuilder extends ModelQueryBuilder {
	ViatraModelQueryBuilder engineOptions(ViatraQueryEngineOptions engineOptions);

	ViatraModelQueryBuilder defaultHint(QueryEvaluationHint queryEvaluationHint);

	ViatraModelQueryBuilder backend(IQueryBackendFactory queryBackendFactory);

	ViatraModelQueryBuilder cachingBackend(IQueryBackendFactory queryBackendFactory);

	ViatraModelQueryBuilder searchBackend(IQueryBackendFactory queryBackendFactory);

	@Override
	default ViatraModelQueryBuilder queries(Dnf... queries) {
		ModelQueryBuilder.super.queries(queries);
		return this;
	}

	@Override
	default ViatraModelQueryBuilder queries(Collection<Dnf> queries) {
		ModelQueryBuilder.super.queries(queries);
		return this;
	}

	@Override
	ViatraModelQueryBuilder query(Dnf query);

	ViatraModelQueryBuilder query(Dnf query, QueryEvaluationHint queryEvaluationHint);

	ViatraModelQueryBuilder computeHint(Function<Dnf, QueryEvaluationHint> computeHint);

	ViatraModelQueryBuilder hint(Dnf dnf, QueryEvaluationHint queryEvaluationHint);

	@Override
	ViatraModelQueryStoreAdapter createStoreAdapter(ModelStore store);
}
