/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.query.interpreter.internal;

import tools.refinery.interpreter.CancellationToken;
import tools.refinery.interpreter.api.AdvancedInterpreterEngine;
import tools.refinery.logic.dnf.AnyQuery;
import tools.refinery.logic.dnf.Query;
import tools.refinery.store.model.Model;
import tools.refinery.store.model.ModelListener;
import tools.refinery.store.query.interpreter.QueryInterpreterAdapter;
import tools.refinery.store.query.resultset.AnyResultSet;
import tools.refinery.store.query.resultset.ResultSet;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.Instant;
import java.util.Map;

public class QueryInterpreterAdapterImpl implements QueryInterpreterAdapter, ModelListener {
	private final Model model;
	private final QueryInterpreterStoreAdapterImpl storeAdapter;
	private final AdvancedInterpreterEngine queryEngine;
	private final Map<AnyQuery, AnyResultSet> resultSets;
	private boolean pendingChanges;

	QueryInterpreterAdapterImpl(Model model, QueryInterpreterStoreAdapterImpl storeAdapter) {
		this.model = model;
		this.storeAdapter = storeAdapter;
		var scope = new RelationalScope(this);
		queryEngine = AdvancedInterpreterEngine.createUnmanagedEngine(scope,
				storeAdapter.getEngineOptions());
		resultSets = storeAdapter.getValidatedQueries().instantiate(this, queryEngine);
		model.addListener(this);
	}

	@Override
	public Model getModel() {
		return model;
	}

	@Override
	public QueryInterpreterStoreAdapterImpl getStoreAdapter() {
		return storeAdapter;
	}

	public CancellationToken getCancellationToken() {
		return storeAdapter.getCancellationToken();
	}

	@Override
	public <T> ResultSet<T> getResultSet(Query<T> query) {
		var canonicalQuery = storeAdapter.getCanonicalQuery(query);
		var resultSet = resultSets.get(canonicalQuery);
		if (resultSet == null) {
			throw new IllegalArgumentException("No matcher for query %s in model".formatted(query.name()));
		}
		@SuppressWarnings("unchecked")
		var typedResultSet = (ResultSet<T>) resultSet;
		return typedResultSet;
	}

	@Override
	public Map<AnyQuery, AnyResultSet> getResultSets() {
		return resultSets;
	}

	@Override
	public boolean hasPendingChanges() {
		return pendingChanges;
	}

	public void markAsPending() {
		if (!pendingChanges) {
			pendingChanges = true;
		}
	}

	@Override
	public void flushChanges() {
//		System.out.println("Flushing changes...");
		queryEngine.flushChanges();
		pendingChanges = false;

		saveResultSets();

//		for(var x : resultSets.entrySet()) {
//			if(x.getValue().size()>0) {
//				System.out.println(x.getKey().name() + " -> " + x.getValue().size());
//			}
//		}
	}

	private void saveResultSets() {
		File csvOutputFile = new File("query-results-all.csv");
		boolean needHeader = !csvOutputFile.exists() || csvOutputFile.length() == 0L;
		try (FileWriter fw = new FileWriter(csvOutputFile, true)) {
			if (needHeader) {
				fw.write(headerLine());
				fw.write("\n");
			}
			fw.write(resultSetToString());
			fw.write("\n");
		} catch (IOException e) {
			throw new java.io.UncheckedIOException(e);
		}
	}

	private String headerLine() {
		StringBuilder sb = new StringBuilder();
		sb.append("timestamp");
		// state hash column
		sb.append(",stateHash");
		for (Map.Entry<AnyQuery, AnyResultSet> entry : resultSets.entrySet()) {
			sb.append(',');
			// use query name as column header
			sb.append(escapeCsv(entry.getKey().name()));
		}
		return sb.toString();
	}

	private String resultSetToString() {
		StringBuilder sb = new StringBuilder();
		// ISO-8601 timestamp
		sb.append(Instant.now().toString());
		// append model state hash code (0 if state is null)
		sb.append(',');
		int stateHash = (model.getState() == null) ? 0 : model.getState().hashCode();
		sb.append(stateHash);
		for (Map.Entry<AnyQuery, AnyResultSet> entry : resultSets.entrySet()) {
			sb.append(',');
			AnyResultSet rs = entry.getValue();
			long size = rs == null ? 0L : rs.size();
			sb.append(size);
		}
		return sb.toString();
	}

	private String escapeCsv(String s) {
		if (s == null) return "";
		if (s.contains(",") || s.contains("\"") || s.contains("\n")) {
			// escape by wrapping in quotes and doubling internal quotes
			return '"' + s.replace("\"", "\"\"") + '"';
		}
		return s;
	}

	@Override
	public void afterRestore() {
		flushChanges();
	}

	@Override
	public void beforeClose() {
		queryEngine.dispose();
	}
}
