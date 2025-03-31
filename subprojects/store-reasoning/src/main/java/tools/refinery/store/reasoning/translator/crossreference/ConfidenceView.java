/*
 * SPDX-FileCopyrightText: 2023-2024 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.reasoning.translator.crossreference;

import tools.refinery.logic.term.Parameter;
import tools.refinery.logic.term.ParameterDirection;
import tools.refinery.logic.term.truthvalue.TruthValue;
import tools.refinery.logic.term.truthvalue.TruthValueConfidence;
import tools.refinery.store.query.view.AbstractFunctionView;
import tools.refinery.store.representation.Symbol;
import tools.refinery.store.tuple.Tuple;

public class ConfidenceView extends AbstractFunctionView<TruthValueConfidence> {
	protected ConfidenceView(Symbol<TruthValueConfidence> symbol, String name) {
		super(symbol, name, new Parameter(Double.class, ParameterDirection.OUT));
	}

	@Override
	protected boolean doFilter(Tuple key, TruthValueConfidence value) {
		return value.getTruthValue() == TruthValue.UNKNOWN;
	}

	@Override
	protected Object forwardMapValue(TruthValueConfidence value) {
		return value.getConfidence();
	}
}
