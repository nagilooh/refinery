/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */

plugins {
	id("tools.refinery.gradle.java-library")
}

mavenArtifact {
	name = "Store Manual DSE"
	description = "Manual Design-space explorer for the model store"
}

dependencies {
	implementation(project(":refinery-language-semantics"))
	implementation(project(":refinery-store-dse"))
	implementation(project(":refinery-store-dse-visualization"))
}
