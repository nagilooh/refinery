/*
 * SPDX-FileCopyrightText: 2021-2023 The Refinery Authors <https://refinery.tools/>
 *
 * SPDX-License-Identifier: EPL-2.0
 */

rootProject.name = "refinery"

include(
		"docs",
		"benchmark",
		"frontend",
		"generator",
		"generator-cli",
		"interpreter",
		"interpreter-localsearch",
		"interpreter-rete",
		"interpreter-rete-recipes",
		"language",
		"language-ide",
		"language-model",
		"language-semantics",
		"language-web",
		"logic",
		"store",
		"store-dse",
		"store-dse-visualization",
		"store-query",
		"store-query-interpreter",
		"store-reasoning",
		"store-reasoning-scope",
		"store-reasoning-smt",
)

for (project in rootProject.children) {
	val projectName = project.name
	project.name = "${rootProject.name}-$projectName"
	project.projectDir = file("subprojects/$projectName")
}
