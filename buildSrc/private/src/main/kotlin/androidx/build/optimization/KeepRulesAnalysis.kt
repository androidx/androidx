/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.build.optimization

import androidx.build.AndroidXExtension
import androidx.build.CompilationTarget
import androidx.build.ProjectLayoutType
import androidx.build.Release
import androidx.build.SoftwareType
import androidx.build.addToBuildOnServer
import androidx.build.addToCheckTask
import androidx.build.androidcli.AndroidCliBuildService
import androidx.build.getPrebuiltsRoot
import androidx.build.uptodatedness.cacheEvenIfNoOutputs
import com.android.build.api.artifact.SingleArtifact
import com.android.build.api.variant.LibraryVariant
import java.io.File
import org.gradle.api.Project
import org.gradle.api.attributes.Category
import org.gradle.api.file.RegularFile
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.TaskProvider
import org.gradle.jvm.tasks.Jar

/** Path to the JSON keep rules baseline file relative to `frameworks/support`. */
internal const val KEEP_RULES_BASELINE_RELATIVE_PATH = "buildSrc/keep-rules-baseline.json"

/**
 * Category attribute value used to aggregate per-project keep rules analysis reports into the root
 * [UpdateKeepRulesBaselineTask].
 */
internal const val KEEP_RULES_ANALYSIS_REPORTS_CATEGORY = "androidx-keep-rules-analysis-reports"

/**
 * Determines whether consumer keep rule analysis should be configured for this project.
 *
 * Automatically includes published device libraries (both AAR and JAR) and deterministically
 * excludes test and benchmark libraries, sample projects, and stub projects.
 */
internal fun Project.shouldConfigureKeepRulesAnalysis(
    androidXExtension: AndroidXExtension
): Boolean {
    if (ProjectLayoutType.isPlayground(this)) {
        return false
    }
    val type = androidXExtension.type.get()
    return type.publish.shouldPublish() &&
        type.compilationTarget == CompilationTarget.DEVICE &&
        // Excludes SoftwareType test/benchmark types (e.g. PUBLISHED_TEST_LIBRARY,
        // INTERNAL_TEST_LIBRARY, BENCHMARK).
        !type.allowCallingVisibleForTestsApis &&
        type != SoftwareType.SAMPLES &&
        type != SoftwareType.STUBS
}

/**
 * Registers [AnalyzeKeepRulesTask] and [ValidateKeepRulesTask] for a published Android library
 * variant, exposes its report for the root [UpdateKeepRulesBaselineTask], and wires validation into
 * `check` and `buildOnServer`.
 */
internal fun Project.configureKeepRulesAnalysis(
    variant: LibraryVariant,
    androidXExtension: AndroidXExtension,
) {
    // Standard `com.android.library` projects publish the "release" (DEFAULT_PUBLISH_CONFIG)
    // variant, whereas Kotlin Multiplatform Android libraries
    // (`com.android.kotlin.multiplatform.library`)
    // have no build-type variants and instead publish a single variant named "androidMain".
    if (variant.name != Release.DEFAULT_PUBLISH_CONFIG && variant.name != "androidMain") {
        return
    }
    configureKeepRulesAnalysisTasks(variant.artifacts.get(SingleArtifact.AAR), androidXExtension)
}

/**
 * Registers [AnalyzeKeepRulesTask] and [ValidateKeepRulesTask] for a published non-Android JVM/JAR
 * library, exposes its report for the root [UpdateKeepRulesBaselineTask], and wires validation into
 * `check` and `buildOnServer`.
 */
internal fun Project.configureKeepRulesAnalysisForJar(
    jarTask: TaskProvider<Jar>,
    androidXExtension: AndroidXExtension,
) {
    configureKeepRulesAnalysisTasks(jarTask.flatMap { it.archiveFile }, androidXExtension)
}

private fun Project.configureKeepRulesAnalysisTasks(
    artifactProvider: Provider<RegularFile>,
    androidXExtension: AndroidXExtension,
) {
    // Defer task registration to afterEvaluate because `androidXExtension.type` is configured by
    // the project's build.gradle after plugin application.
    afterEvaluate {
        if (!shouldConfigureKeepRulesAnalysis(androidXExtension)) {
            return@afterEvaluate
        }
        val cliServiceProvider = AndroidCliBuildService.registerOrGet(project)
        val baselineServiceProvider = KeepRulesBaselineService.registerOrGet(project)
        val versionFile = File(getPrebuiltsRoot(), "androidx/android-cli/VERSION")
        val projectPath = project.path

        // 1. Run `android analyze --analysis=optimization -f json` and write `analysis.json`.
        val analyzeTask =
            tasks.register(AnalyzeKeepRulesTask.TASK_NAME, AnalyzeKeepRulesTask::class.java) { task
                ->
                task.artifact.set(artifactProvider)
                task.projectPath.set(projectPath)
                task.androidCliVersionFile.set(versionFile)
                task.androidCliService.set(cliServiceProvider)
                task.usesService(cliServiceProvider)
                task.reportFile.set(
                    layout.buildDirectory.file("intermediates/keepRulesAnalysis/analysis.json")
                )
            }

        // 2. Validate `analysis.json` against default thresholds and
        // `buildSrc/keep-rules-baseline.json`, and hook validation into `check` and
        // `buildOnServer`.
        val validateTask =
            tasks.register(ValidateKeepRulesTask.TASK_NAME, ValidateKeepRulesTask::class.java) {
                task ->
                task.reportFile.set(analyzeTask.flatMap { it.reportFile })
                task.baseline.set(baselineServiceProvider.map { it.baselineFor(projectPath) })
                task.usesService(baselineServiceProvider)
                task.cacheEvenIfNoOutputs()
            }

        // 3. Expose `analysis.json` via a consumable configuration so the root project's
        // `updateKeepRulesBaseline` task can aggregate all subproject reports.
        val elements =
            configurations.register("keepRulesAnalysisReportElements") { conf ->
                conf.isCanBeConsumed = true
                conf.isCanBeResolved = false
                conf.attributes { attrs ->
                    attrs.attribute(
                        Category.CATEGORY_ATTRIBUTE,
                        objects.named(Category::class.java, KEEP_RULES_ANALYSIS_REPORTS_CATEGORY),
                    )
                }
            }
        artifacts.add(elements.name, analyzeTask.flatMap { it.reportFile })

        addToCheckTask(validateTask)
        addToBuildOnServer(validateTask)
    }
}
