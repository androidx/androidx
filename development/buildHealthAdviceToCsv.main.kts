#!/usr/bin/env kotlin

/*
 * Copyright 2024 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/**
 * To run .kts files, follow these steps:
 *
 * 1. Download and install the Kotlin compiler (kotlinc). There are several ways to do this; see
 *    https://kotlinlang.org/docs/command-line.html
 * 2. Run the script from the command line:
 *    <path_to>/kotlinc -script <script_file>.kts <arguments>
 */

@file:Repository("https://repo1.maven.org/maven2")
@file:DependsOn("com.google.code.gson:gson:2.11.0")

import java.io.File
import java.util.Locale
import kotlin.system.exitProcess
import com.google.gson.Gson

if (args.isEmpty() || !args.contains("-input")) {
    println("Expected path to buildHealth Advice json file using -input.")
    println("Provide output file path using -output. By default, output.csv will be generated " +
        "in the current directory")
    println("Usage ex: kotlinc -script buildHealthAdviceToCsv.main.kts -- -input " +
        "path/to/build-health-report.json -output /path/to/output.csv")
    exitProcess(1)
}

val input = args[1]
if (!File(input).exists()) {
    println("Could not find input files: $input")
    exitProcess(1)
}
val inputJsonFile = File(input)
println("Parsing ${inputJsonFile.path}...")

val outputFile = if(args.contains("-output")) {
    args[3]
} else {
    "output.csv"
}

val csvOutputFile = File(outputFile)
if(csvOutputFile.exists()) {
    csvOutputFile.delete()
}

val csvData = StringBuilder()
val columnHeaders = listOf(
    "ID", // leave blank
    "Priority",
    "Summary",
    "HotlistIds",
    "Assignee", // leave blank
    "Status",
    "ComponentPath",
    "Reporter",
    "FirstNote"
)
csvData.append(columnHeaders.joinToString(","))
csvData.append("\n")

val gson = Gson()
val advice: Advice = gson.fromJson(inputJsonFile.readLines().first(), Advice::class.java)

//list of projects we want to file issues for
//val androidxProjects = listOf("lint", "lint-checks", "buildSrc-tests", "androidx-demos", "stableaidl", "test")
//val flanProjects = listOf("activity", "fragment", "lifecycle", "navigation")

var numProjects = 0
advice.projectAdvice.forEach projectAdvice@{ projectAdvice ->
    val projectPath = projectAdvice.projectPath
    val title = "Please fix misconfigured dependencies for $projectPath"

    val project = projectPath.split(":")[1]

//    Uncomment the following section if you want to create bugs for specific projects
//    if(project !in flanProjects) {
//        return@projectAdvice
//    }

    // Ignore advice for lint projects: b/350084892
    if (projectPath.contains("lint")) {
        return@projectAdvice
    }

    val description = StringBuilder()
    description.appendLine(
        "The dependency analysis gradle plugin found some dependencies that may have been " +
            "misconfigured. Please fix the following dependencies: \n"
    )

    val unused = mutableSetOf<String>()
    val transitive = mutableSetOf<String>()
    val modified = mutableSetOf<String>()
    projectAdvice.dependencyAdvice.forEach { dependencyAdvice ->
        val fromConfiguration = dependencyAdvice.fromConfiguration
        val toConfiguration = dependencyAdvice.toConfiguration
        val coordinates = dependencyAdvice.coordinates
        val resolvedVersion = coordinates.resolvedVersion

        val isCompileOnly = toConfiguration?.endsWith("compileOnly", ignoreCase = true) == true
        val isModifyDependencyAdvice = fromConfiguration != null && toConfiguration != null
        val isTransitiveDependencyAdvice = fromConfiguration == null && toConfiguration != null && !isCompileOnly
        val isUnusedDependencyAdvice = fromConfiguration != null && toConfiguration == null

        // Ignore advice for androidx.profileinstaller:profileinstaller.
        // It needs to remain implementation as that needs to be part of the manifest merger
        // which is before runtime (b/355239547)
        if(coordinates.identifier == "androidx.profileinstaller:profileinstaller") {
            return@forEach
        }

        var identifier = if(resolvedVersion == null) {
            "'${coordinates.identifier}'"
        } else {
            "'${coordinates.identifier}:${coordinates.resolvedVersion}'"
        }
        if (coordinates.type == "project") {
            identifier = "project($identifier)"
        }
        if (isModifyDependencyAdvice) {
            modified.add("$toConfiguration($identifier) (was $fromConfiguration)")
        }
        if(isTransitiveDependencyAdvice) {
            transitive.add("$toConfiguration($identifier)")
        }
        if(isUnusedDependencyAdvice) {
            unused.add("$fromConfiguration($identifier)")
        }
    }

    if(unused.isNotEmpty()) {
        description.appendLine("Unused dependencies which should be removed:")
        description.appendLine("```")
        description.appendLine(unused.sorted().joinToString(separator = "\n"))
        description.appendLine("```")

    }
    if (transitive.isNotEmpty()) {
        description.appendLine("These transitive dependencies can be declared directly:")
        description.appendLine("```")
        description.appendLine(transitive.sorted().joinToString(separator = "\n"))
        description.appendLine("```")
    }
    if (modified.isNotEmpty()) {
        description.appendLine(
            "These dependencies can be modified to be as indicated. Please be careful " +
                "while changing the type of dependencies since it can affect the consumers of " +
                "this library. To learn more about the various dependency configurations, " +
                "please visit: [dac]" +
                "(https://developer.android.com/build/dependencies#dependency_configurations). " +
                "Also check [Gradle's guide for dependency management]" +
                "(https://docs.gradle.org/current/userguide/dependency_management.html).\n"
        )
        description.appendLine("```")
        description.appendLine(modified.sorted().joinToString(separator = "\n"))
        description.appendLine("```")
    }

    description.appendLine("To get more up-to-date project advice, please run:")
    description.appendLine("```")
    description.appendLine("./gradlew $projectPath:projectHealth")
    description.appendLine("```")
    val newColumns = listOf(
        "NEW00000", // ID
        "P2", // priority
        title,
        "=HYPERLINK(\"https://issuetracker.google.com/issues?q=status%3Aopen%20hotlistid%3A(5997499)\", \"Androidx misconfigured dependencies\")",
        "", // Assignee: leave blank
        "Assigned", // status
        "Android > Android OS & Apps > Jetpack (androidx) > ${project.replaceFirstChar {
            if (it.isLowerCase()) it.titlecase(
                Locale.getDefault()
            ) else it.toString()
        }}",
        "", // reporter: add your ldap here
        description.toString()
    )

    if (projectAdvice.dependencyAdvice.isNotEmpty()) {
        numProjects++
        csvData.append(newColumns.joinToString(",") { data ->
            "\"${data.replace("\"", "\"\"")}\""
        })
        csvData.append("\n")
    }
}

csvOutputFile.appendText(csvData.toString())
println("Wrote CSV output to ${csvOutputFile.path} for $numProjects projects")

data class Advice(
    val projectAdvice: List<ProjectAdvice>,
)

data class ProjectAdvice(
    val projectPath: String,
    val dependencyAdvice: List<DependencyAdvice>,
    val pluginAdvice: List<PluginAdvice>,
)

data class DependencyAdvice(
    val coordinates: Coordinates,
    val fromConfiguration: String?,
    val toConfiguration: String?
)

data class Coordinates(
    val type: String,
    val identifier: String,
    val resolvedVersion: String?
)

data class PluginAdvice(
    val redundantPlugin: String,
    val reason: String
)
