/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */

plugins {
	id("tools.refinery.gradle.java-library")
	id("tools.refinery.gradle.jmh")
}

mavenArtifact {
	description = "Benchmark"
}

dependencies {
	implementation("org.eclipse.emf:org.eclipse.emf.ecore:2.36.0")
	implementation("org.eclipse.emf:org.eclipse.emf.ecore.change:2.16.0")
	implementation("org.eclipse.emf:org.eclipse.emf.ecore.xmi:2.37.0")
	implementation("com.mysql:mysql-connector-j:8.4.0")
	implementation("org.apache.commons:commons-exec:1.4.0")
	implementation("commons-io:commons-io:2.16.1")
	api(project(":refinery-store"))
}
