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

import androidx.build.androidcli.AndroidCliBuildService
import androidx.build.androidcli.AndroidCliPack
import java.io.File
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.services.ServiceReference
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction

/**
 * Runs `android analyze --analysis=optimization -f json` on a library artifact (`.aar` or `.jar`)
 * and writes a JSON [KeepRulesAnalysisReport].
 */
@CacheableTask
internal abstract class AnalyzeKeepRulesTask : DefaultTask() {

    @get:InputFile
    @get:PathSensitive(PathSensitivity.NONE)
    abstract val artifact: RegularFileProperty

    /**
     * Gradle project path (e.g. `:core:core`) serialized into [reportFile] so the root
     * [UpdateKeepRulesBaselineTask] can attribute each aggregated report to its source project.
     */
    @get:Input abstract val projectPath: Property<String>

    @get:InputFile
    @get:PathSensitive(PathSensitivity.NONE)
    abstract val androidCliVersionFile: RegularFileProperty

    @get:ServiceReference abstract val androidCliService: Property<AndroidCliBuildService>

    @get:OutputFile abstract val reportFile: RegularFileProperty

    @TaskAction
    fun analyze() {
        val artifactFile = artifact.get().asFile
        val rawJson = analyzeLibraryOptimizationJson(artifactFile)
        val parsed =
            KeepRulesJson.gson.fromJson(rawJson, AnalyzeCommandOutput::class.java)
                ?: throw GradleException(
                    "Failed to parse JSON output from android analyze for ${projectPath.get()}:\n$rawJson"
                )
        val report =
            KeepRulesAnalysisReport(projectPath = projectPath.get(), results = parsed.results)

        val outFile = reportFile.get().asFile
        outFile.parentFile?.mkdirs()
        outFile.writeText(KeepRulesJson.prettyGson.toJson(report))
    }

    private fun analyzeLibraryOptimizationJson(artifactFile: File): String =
        androidCliService
            .get()
            .execute(
                args =
                    listOf(
                        "analyze",
                        "--analysis=optimization",
                        "-f",
                        "json",
                        artifactFile.absolutePath,
                    ),
                requiredPacks = setOf(AndroidCliPack.OPTIMIZATION),
            )

    companion object {
        const val TASK_NAME = "analyzeKeepRules"
    }
}
