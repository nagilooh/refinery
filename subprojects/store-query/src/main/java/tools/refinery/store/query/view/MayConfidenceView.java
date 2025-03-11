/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.query.view;

import tools.refinery.logic.term.truthvalue.TruthValue;
import tools.refinery.logic.term.truthvalue.TruthValueConfidence;
import tools.refinery.store.representation.Symbol;
import tools.refinery.store.tuple.Tuple;

public class MayConfidenceView extends TuplePreservingView<TruthValueConfidence> {
	public MayConfidenceView(Symbol<TruthValueConfidence> symbol) {
		super(symbol, "may");
	}

	@Override
	protected boolean doFilter(Tuple key, TruthValueConfidence value) {
		return value.may();
	}
}
