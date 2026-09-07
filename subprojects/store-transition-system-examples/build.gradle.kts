/*
 * SPDX-FileCopyrightText: 2021-2026 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */

plugins {
	id("tools.refinery.gradle.java-library")
}

mavenArtifact {
	description = "Partial transition system for the model store"
}

dependencies {
	api(project(":refinery-language-semantics"))
	api(project(":refinery-store-dse"))
	api(project(":refinery-store-reasoning"))
	api(project(":refinery-store-transition-system"))
	api(project(":refinery-store-dse-visualization"))
	api(project(":refinery-store-query-interpreter"))
	testImplementation(testFixtures(project(":refinery-logic")))
	testImplementation(testFixtures(project(":refinery-language")))
}
