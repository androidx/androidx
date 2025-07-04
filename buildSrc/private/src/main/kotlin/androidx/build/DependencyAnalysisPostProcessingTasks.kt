/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.build

import androidx.build.logging.TERMINAL_RED
import androidx.build.logging.TERMINAL_RESET
import androidx.build.uptodatedness.cacheEvenIfNoOutputs
import com.autonomousapps.AbstractPostProcessingTask
import com.autonomousapps.model.ModuleCoordinates
import com.autonomousapps.model.ProjectAdvice
import com.autonomousapps.model.ProjectCoordinates
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import java.io.File
import kotlin.text.appendLine
import org.gradle.api.Project
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction

/**
 * Task that reports dependency analysis advice for the project. It gets advice from the dependency
 * analysis gradle plugin and checks the baselines for the advice already captured and only reports
 * if additional violations are found.
 */
@CacheableTask
abstract class ReportDependencyAnalysisAdviceTask : AbstractPostProcessingTask() {
    init {
        group = "Verification"
        description = "Task for generating advice for dependency analysis"
    }

    @get:Internal abstract val baseLineFile: RegularFileProperty

    @InputFile
    @Optional
    @PathSensitive(PathSensitivity.NONE)
    fun getDependencyAnalysisBaseline(): File? = baseLineFile.get().asFile.takeIf { it.exists() }

    @get:Internal val projectPath: String = project.path
    @get:Internal val isKMP: Boolean = project.multiplatformExtension != null
    @get:Internal
    val isPublishedLibrary: Boolean =
        project.extensions.getByType(AndroidXExtension::class.java).type ==
            SoftwareType.PUBLISHED_LIBRARY

    @TaskAction
    fun getAdvice() {
        val projectAdvice =
            this@ReportDependencyAnalysisAdviceTask.projectAdvice().toAndroidxProjectAdvice()

        val baselineAdvice =
            Gson()
                .fromJson(
                    getDependencyAnalysisBaseline()?.readText(),
                    AndroidxProjectAdvice::class.java,
                )

        val advice =
            if (baselineAdvice != null) {
                getIncrementalAdvice(
                    projectAdvice.dependencyAdvice.filter {
                        !baselineAdvice.dependencyAdvice.contains(it)
                    }
                )
            } else {
                getIncrementalAdvice(projectAdvice.dependencyAdvice)
            }

        if (advice.isNotBlank()) {
            error(
                """
                    There are some new dependencies added to this change that might be misconfigured:
                    $advice
                    ********************************************************************************
                    $TERMINAL_RED
                    To get a complete list of misconfigured dependencies, please run:
                    ./gradlew $projectPath:projectHealth.
                    To update the dependency analysis baseline file, please run:
                    ./gradlew $projectPath:updateDependencyAnalysisBaseline
                    $TERMINAL_RESET
                    ********************************************************************************
                """
                    .trimIndent()
            )
        }
    }

    private fun getIncrementalAdvice(missingDependencyAdvice: List<DependencyAdvice>): String {
        // Skip the reporting of modify dependencies for now, so that advice is easier to follow.
        val unused = mutableSetOf<String>()
        val transitive = mutableSetOf<String>()
        val advice = StringBuilder()

        missingDependencyAdvice.forEach {
            // Don't fail CI if test source set has misconfigured dependencies
            if (it.fromConfiguration?.contains("test", ignoreCase = true) == true) {
                return@forEach
            }
            if (it.toConfiguration?.contains("test", ignoreCase = true) == true) {
                return@forEach
            }

            val isCompileOnly =
                it.toConfiguration?.endsWith("compileOnly", ignoreCase = true) == true
            val isTransitiveDependencyAdvice =
                it.fromConfiguration == null && it.toConfiguration != null && !isCompileOnly
            val isUnusedDependencyAdvice =
                it.fromConfiguration != null && it.toConfiguration == null

            val identifier =
                if (it.coordinates.type == "project") {
                    "project(${it.coordinates.identifier})"
                } else {
                    "'${it.coordinates.identifier}:${it.coordinates.resolvedVersion}'"
                }
            if (isTransitiveDependencyAdvice) {
                transitive.add("${it.toConfiguration}($identifier)")
            }
            if (isUnusedDependencyAdvice) {
                unused.add("${it.fromConfiguration}($identifier)")
            }
        }
        if (unused.isNotEmpty()) {
            advice.appendLine("Unused dependencies which should be removed:")
            advice.appendLine(unused.sorted().joinToString(separator = "\n"))
        }
        if (transitive.isNotEmpty()) {
            advice.appendLine("These transitive dependencies can be declared directly:")
            advice.appendLine(transitive.sorted().joinToString(separator = "\n"))
        }
        return advice.toString()
    }
}

/** Task to update dependency analysis baselines for the project. */
@CacheableTask
abstract class UpdateDependencyAnalysisBaseLineTask : AbstractPostProcessingTask() {
    init {
        group = "Verification"
        description = "Task for updating dependency analysis baselines"
    }

    @get:OutputFile abstract val outputFile: RegularFileProperty
    @get:Internal val isKMP: Boolean = project.multiplatformExtension != null
    @get:Internal
    val isPublishedLibrary: Boolean =
        project.extensions.getByType(AndroidXExtension::class.java).type ==
            SoftwareType.PUBLISHED_LIBRARY

    @TaskAction
    fun updateBaseLineForDependencyAnalysisAdvice() {
        val projectAdvice =
            this@UpdateDependencyAnalysisBaseLineTask.projectAdvice().toAndroidxProjectAdvice()
        val outputFile = outputFile.get()
        val gson = GsonBuilder().setPrettyPrinting().create()
        outputFile.asFile.writeText(gson.toJson(projectAdvice))
    }
}

/**
 * Configure the dependency analysis gradle plugin and register new post-processing tasks:
 * 1. Updating the baselines for advice provided by the plugin.
 * 2. Getting any incremental advice not captured in the baselines.
 */
internal fun Project.configureDependencyAnalysisPlugin() {
    plugins.apply("com.autonomousapps.dependency-analysis")

    val updateDependencyAnalysisBaselineTask =
        tasks.register(
            "updateDependencyAnalysisBaseline",
            UpdateDependencyAnalysisBaseLineTask::class.java,
        ) { task ->
            task.outputFile.set(layout.projectDirectory.file("dependencyAnalysis-baseline.json"))
            task.cacheEvenIfNoOutputs()
            // DAGP currently doesn't support KMP, enable KMP projects when b/394970486 is resolved
            task.onlyIf { !(task.isKMP) && task.isPublishedLibrary }
        }

    val reportDependencyAnalysisAdviceTask =
        tasks.register(
            "reportDependencyAnalysisAdvice",
            ReportDependencyAnalysisAdviceTask::class.java,
        ) { task ->
            task.baseLineFile.set(layout.projectDirectory.file("dependencyAnalysis-baseline.json"))
            task.cacheEvenIfNoOutputs()
            // DAGP currently doesn't support KMP, enable KMP projects when b/394970486 is resolved
            task.onlyIf { !(task.isKMP) && task.isPublishedLibrary }
        }

    val dependencyAnalysisSubExtension =
        extensions.getByType(com.autonomousapps.DependencyAnalysisSubExtension::class.java)
    dependencyAnalysisSubExtension.registerPostProcessingTask(reportDependencyAnalysisAdviceTask)
    dependencyAnalysisSubExtension.registerPostProcessingTask(updateDependencyAnalysisBaselineTask)

    // Ignore advice for runTimeOnly, compileOnly or incorrect dependency configs
    // since it affects downstream consumers
    dependencyAnalysisSubExtension.issues { it.onIncorrectConfiguration { it.severity("ignore") } }
    dependencyAnalysisSubExtension.issues { it.onRuntimeOnly { it.severity("ignore") } }
    dependencyAnalysisSubExtension.issues { it.onCompileOnly { it.severity("ignore") } }

    // DAGP currently doesn't support KMP, enable KMP projects when b/394970486 is resolved
    // Enable CI check for published libraries
    if (
        multiplatformExtension == null && androidXExtension.type == SoftwareType.PUBLISHED_LIBRARY
    ) {
        addToBuildOnServer(reportDependencyAnalysisAdviceTask)
    }
}

/**
 * Helper data classes to store the advice provided Dependency Analysis Gradle plugin in baselines.
 */
internal data class AndroidxProjectAdvice(
    val projectPath: String,
    val dependencyAdvice: List<DependencyAdvice>,
)

internal data class DependencyAdvice(
    val coordinates: Coordinates,
    val fromConfiguration: String?,
    val toConfiguration: String?,
)

internal data class Coordinates(
    val type: String,
    val identifier: String,
    val resolvedVersion: String?,
)

/** Convert advice reported by DAGP into format suitable for storing in baselines. */
internal fun ProjectAdvice.toAndroidxProjectAdvice(): AndroidxProjectAdvice {
    return AndroidxProjectAdvice(
        projectPath = projectPath,
        dependencyAdvice =
            dependencyAdvice.map {
                val type =
                    if (it.coordinates is ProjectCoordinates) {
                        "project"
                    } else {
                        "module"
                    }
                val resolvedVersion =
                    if (it.coordinates is ModuleCoordinates) {
                        (it.coordinates as ModuleCoordinates).resolvedVersion
                    } else {
                        null
                    }
                DependencyAdvice(
                    coordinates =
                        Coordinates(
                            identifier = it.coordinates.identifier,
                            resolvedVersion = resolvedVersion,
                            type = type,
                        ),
                    fromConfiguration = it.fromConfiguration,
                    toConfiguration = it.toConfiguration,
                )
            },
    )
}
