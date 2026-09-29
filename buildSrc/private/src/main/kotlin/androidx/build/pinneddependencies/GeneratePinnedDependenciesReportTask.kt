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
import androidx.build.getLibraryProjectCoordinates
import androidx.build.getTipOfTreeDependencies
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
 * Writes a JSON [UnpinnedDependenciesReport] listing this library's tip-of-tree dependencies that
 * release separately from it, for aggregation by [UpdateTipOfTreeExemptionsTask].
 */
@CacheableTask
internal abstract class GeneratePinnedDependenciesReportTask : DefaultTask() {

    @get:Input abstract val library: Property<ProjectCoordinates>

    @get:Input abstract val tipOfTreeDependencies: SetProperty<ProjectCoordinates>

    @get:OutputFile abstract val reportFile: RegularFileProperty

    @TaskAction
    fun generate() {
        val lib = library.get()
        val report =
            UnpinnedDependenciesReport(
                library = lib.projectPath,
                libraryVersion = lib.version?.toString(),
                unpinnedDependencies =
                    findUnpinnedDependencies(lib, tipOfTreeDependencies.get())
                        .map { it.projectPath }
                        .sorted(),
            )
        val file = reportFile.get().asFile
        file.parentFile?.mkdirs()
        file.writeText(GsonBuilder().setPrettyPrinting().create().toJson(report))
    }

    companion object {
        const val TASK_NAME = "generatePinnedDependenciesReport"
    }
}

/** Artifact category under which each project publishes its [UnpinnedDependenciesReport]. */
internal const val PINNED_DEPENDENCY_REPORTS_CATEGORY = "androidx-pinned-dependency-reports"

/**
 * Registers [GeneratePinnedDependenciesReportTask] and exposes its report through a consumable
 * configuration so that the root project can aggregate it.
 */
internal fun Project.configurePinnedDependenciesReport(
    versionService: Provider<LibraryVersionsService>
) {
    val reportTask =
        tasks.register(
            GeneratePinnedDependenciesReportTask.TASK_NAME,
            GeneratePinnedDependenciesReportTask::class.java,
        ) { task ->
            task.library.set(getLibraryProjectCoordinates(versionService))
            task.tipOfTreeDependencies.set(getTipOfTreeDependencies(versionService))
            task.reportFile.set(
                layout.buildDirectory.file("intermediates/pinnedDependencies/report.json")
            )
        }
    val elements =
        configurations.register("pinnedDependencyReportElements") { conf ->
            conf.isCanBeConsumed = true
            conf.isCanBeResolved = false
            conf.attributes { attrs ->
                attrs.attribute(
                    Category.CATEGORY_ATTRIBUTE,
                    objects.named(Category::class.java, PINNED_DEPENDENCY_REPORTS_CATEGORY),
                )
            }
        }
    artifacts.add(elements.name, reportTask.flatMap { it.reportFile })
}

/** A library's tip-of-tree dependencies that release separately from it. */
internal data class UnpinnedDependenciesReport(
    val library: String,
    val libraryVersion: String?,
    val unpinnedDependencies: List<String>,
)
