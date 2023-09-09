/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.store.dse.logging;

import tools.refinery.store.adapter.ModelStoreAdapter;

import java.util.LinkedHashSet;

public interface LoggingStoreAdapter extends ModelStoreAdapter {
	LinkedHashSet<Logger> getLoggers();
}
