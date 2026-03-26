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
	implementation(libs.jcommander)
	implementation(libs.slf4j)
	implementation("com.github.weisj:jsvg:1.3.0")
	testRuntimeOnly(libs.slf4j.simple)
}

application {
	mainClass.set("tools.refinery.generator.gui.RefineryGui")
}

tasks.register<JavaExec>("gui") {
	val mainRuntimeClasspath = sourceSets.main.map { it.runtimeClasspath }
	dependsOn(mainRuntimeClasspath)
	classpath(mainRuntimeClasspath)
	mainClass.set(application.mainClass)
	// Workaround for https://github.com/gradle/gradle/issues/6074 when running this task from a subdirectory.
	val pwd = System.getProperty("user.dir")
	workingDir = if (pwd == null) rootDir else file(pwd)
	standardInput = System.`in`
	group = "Run"
	description = "Run the Refinery GUI application"
}
