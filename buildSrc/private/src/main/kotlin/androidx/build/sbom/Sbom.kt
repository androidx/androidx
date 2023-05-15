/*
 * Copyright 2023 The Android Open Source Project
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

package androidx.build.sbom

import androidx.build.BundleInsideHelper
import androidx.build.GMavenZipTask
import androidx.inspection.gradle.EXPORT_INSPECTOR_DEPENDENCIES
import androidx.inspection.gradle.IMPORT_INSPECTOR_DEPENDENCIES
import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.tasks.bundling.AbstractArchiveTask
import org.gradle.api.tasks.bundling.Zip
import org.gradle.jvm.tasks.Jar

/**
 * Tells whether the contents of the Configuration with the given name should be listed in our sbom
 *
 * That is, this tells whether the corresponding Configuration contains dependencies that get embedded into our build artifact
 */
fun Project.shouldSbomIncludeConfigurationName(configurationName: String): Boolean {
    return when (configurationName) {
        BundleInsideHelper.CONFIGURATION_NAME -> true
        "shadowed" -> true
        "compileClasspath" -> appliesShadowPlugin()
        EXPORT_INSPECTOR_DEPENDENCIES -> true
        IMPORT_INSPECTOR_DEPENDENCIES -> true
        else -> false
    }
}

// some tasks that don't embed configurations having external dependencies
private val excludeTaskNames = setOf(
    "distZip",
    "shadowDistZip",
    "annotationsZip",
    "protoLiteJar",
    "bundleDebugLocalLintAar",
    "bundleReleaseLocalLintAar",
    "bundleDebugAar",
    "bundleReleaseAar"
)

/**
 * Lists the Configurations that we should declare we're embedding into the output of this task
 *
 * The immediate inputs to the task are not generally mentioned here: external entities aren't
 * interested in knowing that our .aar file contains a classes.jar
 *
 * The external dependencies that embed into our artifacts are what we mention here:
 * external entities might be interested in knowing if,
 * for example, we embed protobuf-javalite into our artifact
 *
 * The purpose of this function is to detect new archive tasks and remind developers to
 * update shouldSbomIncludeConfiguration
 */
fun Project.listSbomConfigurationNamesForArchive(task: AbstractArchiveTask): List<String> {
    if (task is Jar && !(task is ShadowJar)) {
        // Jar tasks don't generally embed other dependencies in them
        return listOf()
    }
    if (task is GMavenZipTask) {
        // A GMavenZipTask just zips one or more artifacts we've already built
        return listOf()
    }

    val projectPath = project.path
    val taskName = task.name

    // some tasks that embed other configurations
    if (taskName == BundleInsideHelper.REPACKAGE_TASK_NAME) {
        return listOf(BundleInsideHelper.CONFIGURATION_NAME)
    }
    if (
        projectPath.contains("inspection") &&
        (
            taskName == "assembleInspectorJarRelease" ||
            taskName == "inspectionShadowDependenciesRelease"
        )
    ) {
        return listOf(EXPORT_INSPECTOR_DEPENDENCIES)
    }

    if (excludeTaskNames.contains(taskName))
        return listOf()
    if (projectPath == ":compose:lint:internal-lint-checks")
        return listOf() // we don't publish these lint checks
    if (projectPath.contains("integration-tests"))
        return listOf() // we don't publish integration tests
    if (taskName.startsWith("zip") && taskName.contains("ResultsOf") && taskName.contains("Test"))
        return listOf() // we don't publish test results
    if (projectPath == ":compose:compiler:compiler" && taskName == "embeddedPlugin")
        return listOf()

    // ShadowJar tasks have a `configurations` property that lists the configurations that
    // are inputs to the task, but they don't also list file inputs
    // If a project only has one shadowJar task (named "shadowJar"), for now we assume
    // that it doesn't include any external files that aren't already declared in
    // its configurations.
    // If a project has multiple shadowJar tasks, we ask the developer to provide
    // this metadata somehow by failing below
    if (taskName == "shadowJar") {
        // If the task is a ShadowJar task, we can just ask it which configurations it intends to embed
        // We separately validate that this list is correct in
        val shadowTask = task as? ShadowJar
        if (shadowTask != null) {
            val configurations = project.configurations.filter { conf ->
               shadowTask.configurations.contains(conf)
            }
            return configurations.map { conf -> conf.name }
        }
    }

    throw GradleException(
        "Not sure which external dependencies are included in $projectPath:$taskName of type " +
        "${task::class.java} (this is used for publishing sboms). Please update " +
        "AndroidXImplPlugin's listSbomConfigurationNamesForArchive and " +
        "shouldSbomIncludeConfiguration"
    )
}

/**
 * Returns which configurations are used by the given task that we should list in an sbom
 */
fun Project.listSbomConfigurationsForArchive(task: AbstractArchiveTask): List<Configuration> {
    val configurationNames = listSbomConfigurationNamesForArchive(task)
    return configurationNames.map { configurationName ->
        val resolved = project.configurations.findByName(configurationName)
        if (resolved == null) {
            throw GradleException(
                "listSbomConfigurationsForArchive($task) expected to find " +
                "configuration $configurationName but it does not exist"
            )
        }
        resolved
    }
}

/**
 * Validates that the inputs of the given archive task are recognized
 */
fun Project.validateArchiveInputsRecognized(task: AbstractArchiveTask) {
    val configurationNames = task.project.listSbomConfigurationNamesForArchive(task)
    for (configurationName in configurationNames) {
        if (!task.project.shouldSbomIncludeConfigurationName(configurationName)) {
            throw GradleException(
                "Task listSbomConfigurationNamesForArchive(\"${task.name}\") = " +
                "$configurationNames but " +
                "shouldSbomIncludeConfigurationName(\"$configurationName\") = false. " +
                "You probably should update shouldSbomIncludeConfigurationName to match"
            )
        }
    }
}

/**
 * Validates that the inputs of each archive task are recognized
 */
fun Project.validateAllArchiveInputsRecognized() {
    project.tasks.withType(Zip::class.java).configureEach { task ->
        project.validateArchiveInputsRecognized(task)
    }
    project.tasks.withType(ShadowJar::class.java).configureEach { task ->
        project.validateArchiveInputsRecognized(task)
    }
}

private fun Project.appliesShadowPlugin() =
    pluginManager.hasPlugin("com.github.johnrengelman.shadow")
