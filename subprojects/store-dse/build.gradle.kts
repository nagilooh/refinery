/*
 * SPDX-FileCopyrightText: 2021-2026 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */

plugins {
	id("tools.refinery.gradle.java-library")
}

mavenArtifact {
	name = "Store DSE"
	description = "Design-space explorer for the model store"
}

dependencies {
	api(project(":refinery-store-query"))
	implementation(libs.eclipseCollections)
	runtimeOnly(libs.eclipseCollections.impl)
	testImplementation(project(":refinery-store-query-interpreter"))
}
