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

import androidx.build.LibraryVersionsService
import androidx.build.getExemptionCandidates
import androidx.build.getLibraryCoordinates
import com.google.gson.GsonBuilder
import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.attributes.Category
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.provider.SetProperty
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction

/**
 * Writes a JSON [DependencyExemptionsReport] listing this library's dependencies that need an
 * exemption, for aggregation by [UpdateDependencyExemptionsTask].
 */
@CacheableTask
internal abstract class GenerateDependencyExemptionsReportTask : DefaultTask() {

    @get:Input abstract val library: Property<LibraryCoordinates>

    @get:Input abstract val exemptionCandidates: SetProperty<LibraryCoordinates>

    @get:OutputFile abstract val reportFile: RegularFileProperty

    @TaskAction
    fun generate() {
        val lib = library.get()
        val report =
            DependencyExemptionsReport(
                library = lib.coordinate,
                libraryVersion = lib.version?.toString(),
                dependenciesNeedingExemption =
                    findDependenciesNeedingExemption(lib, exemptionCandidates.get())
                        .map { it.coordinate }
                        .sorted(),
            )
        val file = reportFile.get().asFile
        file.parentFile?.mkdirs()
        file.writeText(GsonBuilder().setPrettyPrinting().create().toJson(report))
    }

    companion object {
        const val TASK_NAME = "generateDependencyExemptionsReport"
    }
}

/** Artifact category under which each project publishes its [DependencyExemptionsReport]. */
internal const val DEPENDENCY_EXEMPTIONS_REPORTS_CATEGORY = "androidx-dependency-exemptions-reports"

/**
 * Registers [GenerateDependencyExemptionsReportTask] and exposes its report through a consumable
 * configuration so that the root project can aggregate it.
 */
internal fun Project.configureDependencyExemptionsReport(
    versionService: Provider<LibraryVersionsService>
) {
    val reportTask =
        tasks.register(
            GenerateDependencyExemptionsReportTask.TASK_NAME,
            GenerateDependencyExemptionsReportTask::class.java,
        ) { task ->
            task.library.set(getLibraryCoordinates(versionService))
            task.exemptionCandidates.set(getExemptionCandidates(versionService))
            task.reportFile.set(
                layout.buildDirectory.file("intermediates/dependencyExemptions/report.json")
            )
        }
    val elements =
        configurations.register("dependencyExemptionsReportElements") { conf ->
            conf.isCanBeConsumed = true
            conf.isCanBeResolved = false
            conf.attributes { attrs ->
                attrs.attribute(
                    Category.CATEGORY_ATTRIBUTE,
                    objects.named(Category::class.java, DEPENDENCY_EXEMPTIONS_REPORTS_CATEGORY),
                )
            }
        }
    artifacts.add(elements.name, reportTask.flatMap { it.reportFile })
}

/** A library's dependencies that need an exemption. */
internal data class DependencyExemptionsReport(
    val library: String,
    val libraryVersion: String?,
    val dependenciesNeedingExemption: List<String>,
)
