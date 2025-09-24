/*
 * SPDX-FileCopyrightText: 2023-2024 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */
package tools.refinery.generator;

import tools.refinery.store.dse.strategy.ManualStoreManager;
import tools.refinery.store.dse.transition.statespace.ActivationStore;
import tools.refinery.store.map.Version;

public interface ModelExplorer extends ModelFacade {
	ActivationStore.VisitResult step();

	Version getLast();

	ManualStoreManager getStoreManager();
}
