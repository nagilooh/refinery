/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.query.view;

import tools.refinery.logic.term.truthvalue.TruthValueConfidence;
import tools.refinery.store.representation.Symbol;
import tools.refinery.store.tuple.Tuple;

public class MustConfidenceView extends TuplePreservingView<TruthValueConfidence> {
	public MustConfidenceView(Symbol<TruthValueConfidence> symbol) {
		super(symbol, "must");
	}

	@Override
	protected boolean doFilter(Tuple key, TruthValueConfidence value) {
		return value.must();
	}
}
