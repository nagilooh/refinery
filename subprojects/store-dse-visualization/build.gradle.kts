/*
 * SPDX-FileCopyrightText: 2021-2026 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */

plugins {
	id("tools.refinery.gradle.java-library")
}

mavenArtifact {
	name = "Store DSE Visualization"
	description = "Design-space exploration visualizer for the model store"
}

dependencies {
	api(project(":refinery-store-query"))
	implementation(project(":refinery-store-dse"))
	implementation(libs.slf4j)
}
