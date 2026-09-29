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

package androidx.build.pinneddependencies

import androidx.build.Version
import com.google.gson.GsonBuilder
import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.work.DisableCachingByDefault

/**
 * Aggregates candidate tip-of-tree dependency exemptions from all published libraries, prunes
 * stale/obsolete entries that are no longer declared, preserves existing reasons and bugs for
 * active exemptions, and writes [TIP_OF_TREE_EXEMPTIONS_FILE_NAME].
 */
@DisableCachingByDefault(because = "Updates source TOML file in-place")
internal abstract class UpdateTipOfTreeExemptionsTask : DefaultTask() {

    @get:InputFiles
    @get:PathSensitive(PathSensitivity.NONE)
    abstract val reportFiles: ConfigurableFileCollection

    @get:OutputFile abstract val exemptionsFile: RegularFileProperty

    @TaskAction
    fun updateExemptions() {
        val reports = mutableListOf<UnpinnedDependenciesReport>()

        for (file in reportFiles.files) {
            if (!file.exists()) continue
            val report: UnpinnedDependenciesReport? =
                GSON.fromJson(file.readText(), UnpinnedDependenciesReport::class.java)
            if (report != null) {
                reports.add(report)
            }
        }

        val outputFile = exemptionsFile.get().asFile
        val existingExemptions =
            if (outputFile.exists()) {
                parseTipOfTreeExemptions(outputFile.readText(), outputFile.path)
            } else {
                emptyList()
            }
        val updatedExemptions = reconcileExemptions(existingExemptions, reports)

        val tomlContent = renderExemptionsToml(updatedExemptions)
        if (!outputFile.exists() || outputFile.readText() != tomlContent) {
            outputFile.parentFile?.mkdirs()
            outputFile.writeText(tomlContent)
        }
    }

    companion object {
        const val TASK_NAME = "updateTipOfTreeExemptions"
    }
}

/**
 * Returns the exemptions that should exist after evaluating [reports].
 *
 * Libraries without a report were outside the evaluation scope, so their [existing] exemptions are
 * kept untouched. For evaluated libraries, each unpinned dependency keeps its existing exemption
 * (preserving its reason and version) or gets a [placeholderExemption]; exemptions for dependencies
 * that are no longer unpinned are dropped.
 */
internal fun reconcileExemptions(
    existing: List<TipOfTreeExemption>,
    reports: List<UnpinnedDependenciesReport>,
): List<TipOfTreeExemption> {
    val evaluatedLibraries = reports.mapTo(mutableSetOf()) { it.library }
    val existingByPair = existing.associateBy { it.library to it.dependsOn }

    val outOfScope = existing.filter { it.library !in evaluatedLibraries }
    val current = reports.flatMap { report ->
        val libraryVersion = report.libraryVersion?.let { Version.parseOrNull(it) }
        report.unpinnedDependencies.map { dependsOn ->
            existingByPair[report.library to dependsOn]
                ?: placeholderExemption(report.library, libraryVersion, dependsOn)
        }
    }

    return (outOfScope + current).sortedWith(compareBy({ it.library }, { it.dependsOn }))
}

private val GSON = GsonBuilder().disableHtmlEscaping().create()

internal fun renderExemptionsToml(exemptions: List<TipOfTreeExemption>): String {
    val builder = StringBuilder()
    builder.append(
        """
        # Copyright 2026 The Android Open Source Project
        #
        # Licensed under the Apache License, Version 2.0 (the "License");
        # you may not use this file except in compliance with the License.
        # You may obtain a copy of the License at
        #
        #      http://www.apache.org/licenses/LICENSE-2.0
        #
        # Unless required by applicable law or agreed to in writing, software
        # distributed under the License is distributed on an "AS IS" BASIS,
        # WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
        # See the License for the specific language governing permissions and
        # limitations under the License.

        # Tip-of-tree project dependencies that are temporarily exempted from being pinned
        # to released artifact versions.
        """
            .trimIndent()
    )
    builder.append("\n")

    for ((library, dependsOn, validThroughLibraryVersion, reason) in exemptions) {
        builder.append("\n[[tipOfTreeExemptions]]\n")
        builder.append("library = ${GSON.toJson(library)}\n")
        builder.append("dependsOn = ${GSON.toJson(dependsOn)}\n")
        builder.append("validThroughLibraryVersion = ${GSON.toJson(validThroughLibraryVersion)}\n")
        builder.append("reason = ${GSON.toJson(reason)}\n")
    }

    return builder.toString()
}
