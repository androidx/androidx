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

import androidx.build.lazyReadFile
import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.services.BuildService
import org.gradle.api.services.BuildServiceParameters
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.work.DisableCachingByDefault

/**
 * Root-project task that aggregates [KeepRulesAnalysisReport] files from all published libraries
 * via [KEEP_RULES_ANALYSIS_REPORTS_CATEGORY], prunes fixed baseline entries, and writes
 * [KEEP_RULES_BASELINE_RELATIVE_PATH].
 */
@DisableCachingByDefault(because = "Updates source JSON baseline file in-place")
internal abstract class UpdateKeepRulesBaselineTask : DefaultTask() {

    init {
        group = "Verification"
        description =
            "Updates $KEEP_RULES_BASELINE_RELATIVE_PATH from the aggregated android analyze outputs."
    }

    @get:InputFiles
    @get:PathSensitive(PathSensitivity.NONE)
    abstract val reportFiles: ConfigurableFileCollection

    @get:OutputFile abstract val baselineFile: RegularFileProperty

    @TaskAction
    fun updateBaseline() {
        val reports = mutableListOf<KeepRulesAnalysisReport>()
        for (file in reportFiles.files) {
            if (!file.exists()) continue
            val report =
                KeepRulesJson.gson.fromJson(file.readText(), KeepRulesAnalysisReport::class.java)
            if (report != null) {
                reports.add(report)
            }
        }

        val outputFile = baselineFile.get().asFile
        val existingBaselines =
            if (outputFile.exists()) {
                KeepRulesJson.parseBaselineJson(outputFile.readText())
            } else {
                emptyMap()
            }
        val updatedBaselines = reconcileBaselines(existingBaselines, reports)
        val renderedJson = KeepRulesJson.renderBaselineJson(updatedBaselines)
        if (!outputFile.exists() || outputFile.readText() != renderedJson) {
            outputFile.parentFile?.mkdirs()
            outputFile.writeText(renderedJson)
        }
    }

    companion object {
        const val TASK_NAME = "updateKeepRulesBaseline"
    }
}

/**
 * Shared read-only [BuildService] that parses [KEEP_RULES_BASELINE_RELATIVE_PATH] once per build
 * for [ValidateKeepRulesTask].
 */
internal abstract class KeepRulesBaselineService :
    BuildService<KeepRulesBaselineService.Parameters> {

    interface Parameters : BuildServiceParameters {
        val baselineFileContents: Property<String>
    }

    private val baselines: Map<String, BaselineConfig> by lazy {
        KeepRulesJson.parseBaselineJson(parameters.baselineFileContents.orNull)
    }

    fun baselineFor(projectPath: String): BaselineConfig? = baselines[projectPath]

    companion object {
        private const val SERVICE_NAME = "keepRulesBaselineService"

        fun registerOrGet(project: Project): Provider<KeepRulesBaselineService> {
            val baselineContents = project.lazyReadFile(KEEP_RULES_BASELINE_RELATIVE_PATH)
            return project.gradle.sharedServices.registerIfAbsent(
                SERVICE_NAME,
                KeepRulesBaselineService::class.java,
            ) { spec ->
                spec.parameters.baselineFileContents.set(baselineContents)
            }
        }
    }
}

/**
 * Merges [reports] into [existing] baselines.
 *
 * Libraries not present in [reports] keep their existing baseline untouched; libraries present in
 * [reports] have their baseline recomputed (or removed if they now pass all default checks).
 */
internal fun reconcileBaselines(
    existing: Map<String, BaselineConfig>,
    reports: List<KeepRulesAnalysisReport>,
): Map<String, BaselineConfig> {
    val evaluatedProjects = reports.mapTo(mutableSetOf()) { it.projectPath }
    val result = sortedMapOf<String, BaselineConfig>()

    for ((projectPath, config) in existing) {
        if (projectPath !in evaluatedProjects) {
            result[projectPath] = config
        }
    }

    for (report in reports) {
        val dex =
            report.results?.firstOrNull()?.optimization?.libraryAnalysisResult?.dex ?: continue
        val computed = computeBaselineForResult(dex)
        if (computed != null) {
            result[report.projectPath] = computed
        }
    }

    return result
}

private fun computeBaselineForResult(dex: LibraryAnalyzerResultJson): BaselineConfig? {
    val d8SizeBytes = dex.d8CompileResult?.dexSizeBytes ?: 0L
    val r8SizeBytes = dex.r8CompileResult?.dexSizeBytes ?: 0L
    val actualKeepRatio = KeepRulesErrorReporter.computeKeepRatio(d8SizeBytes, r8SizeBytes)
    val overrideMaxKeepRatio =
        if (actualKeepRatio > KeepRulesErrorReporter.DEFAULT_MAX_KEEP_RATIO) {
            KeepRulesErrorReporter.computeBaselineKeepRatio(actualKeepRatio)
        } else {
            null
        }

    val overrideMaxD8SizeMegabytes =
        if (d8SizeBytes > KeepRulesErrorReporter.DEFAULT_D8_MAX_SIZE_BYTES) {
            KeepRulesErrorReporter.computeBaselineMaxD8SizeMegabytes(d8SizeBytes)
        } else {
            null
        }

    val baselineUsedPackageWideKeepRules =
        dex.r8CompileResult
            ?.configuration
            ?.usedPackageWideKeepRules
            .orEmpty()
            .map { it.source.trim() }
            .toSortedSet()
            .takeIf { it.isNotEmpty() }

    val baselineUnusedPackageWideKeepRules =
        dex.r8CompileResult
            ?.configuration
            ?.unusedPackageWideKeepRules
            .orEmpty()
            .map { it.source.trim() }
            .toSortedSet()
            .takeIf { it.isNotEmpty() }

    if (
        overrideMaxKeepRatio == null &&
            overrideMaxD8SizeMegabytes == null &&
            baselineUsedPackageWideKeepRules == null &&
            baselineUnusedPackageWideKeepRules == null
    ) {
        return null
    }

    return BaselineConfig(
        overrideMaxKeepRatio = overrideMaxKeepRatio,
        overrideMaxD8SizeMegabytes = overrideMaxD8SizeMegabytes,
        baselineUsedPackageWideKeepRules = baselineUsedPackageWideKeepRules,
        baselineUnusedPackageWideKeepRules = baselineUnusedPackageWideKeepRules,
    )
}
