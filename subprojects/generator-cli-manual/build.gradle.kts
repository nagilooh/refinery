/*
 * SPDX-FileCopyrightText: 2024 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */

plugins {
	id("tools.refinery.gradle.java-application")
}

dependencies {
	implementation(project(":refinery-generator"))
	implementation(project(":refinery-generator-cli"))
	implementation(project(":refinery-store-dse-manual"))
	implementation(project(":refinery-store-dse-visualization"))
	implementation(libs.jcommander)
	implementation(libs.slf4j)
	testRuntimeOnly(libs.slf4j.simple)
	testImplementation(project(":refinery-store-query-interpreter"))
}

application {
	mainClass.set("tools.refinery.generator.cli.manual.RefineryCliManual")
}

tasks.register<JavaExec>("cli-manual") {
	val mainRuntimeClasspath = sourceSets.main.map { it.runtimeClasspath }
	dependsOn(mainRuntimeClasspath)
	classpath(mainRuntimeClasspath)
	mainClass.set(application.mainClass)
	// Workaround for https://github.com/gradle/gradle/issues/6074 when running this task from a subdirectory.
	val pwd = System.getProperty("user.dir")
	workingDir = if (pwd == null) rootDir else file(pwd)
	standardInput = System.`in`
	group = "Run"
	description = "Run the Refinery CLI application with manual steps"
}
