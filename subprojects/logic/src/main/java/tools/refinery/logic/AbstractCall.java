/*
 * SPDX-FileCopyrightText: 2025 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.logic;

import tools.refinery.logic.term.ParameterDirection;
import tools.refinery.logic.term.Variable;

import java.util.List;
import java.util.Set;

public interface AbstractCall {

	public Constraint getTarget();

	public List<Variable> getArguments();

	Set<Variable> getArgumentsOfDirection(ParameterDirection direction);

}
