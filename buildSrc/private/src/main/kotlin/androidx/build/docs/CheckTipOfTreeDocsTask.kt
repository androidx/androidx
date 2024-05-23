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

package androidx.build.docs

import androidx.build.AndroidXExtension
import androidx.build.INCLUDE_OPTIONAL_PROJECTS
import androidx.build.LibraryType
import androidx.build.addToBuildOnServer
import androidx.build.checkapi.shouldConfigureApiTasks
import androidx.build.getSupportRootFolder
import androidx.build.multiplatformExtension
import androidx.build.uptodatedness.cacheEvenIfNoOutputs
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction

/**
 * Verifies that the text of the [projectPathProvider] can be found in the [tipOfTreeBuildFile] to
 * enforce that projects enable docs generation.
 */
@CacheableTask
abstract class CheckTipOfTreeDocsTask : DefaultTask() {
    @get:[InputFile PathSensitive(PathSensitivity.NONE)]
    abstract val tipOfTreeBuildFile: RegularFileProperty

    @get:Input abstract val projectPathProvider: Property<String>

    @get:Input abstract val includeOptionalProjects: Property<Boolean>

    @get:Input abstract val type: Property<DocsType>

    @TaskAction
    fun exec() {
        val projectPath = projectPathProvider.get()
        // Make sure not to allow a partial project path match, e.g. ":activity:activity" shouldn't
        // match ":activity:activity-ktx", both need to be listed separately.
        val projectDependency = "project(\"$projectPath\")"

        val prefix = type.get().prefix
        // Check that projects are listed with the right configuration type (docs, kmpDocs, samples)
        val fullExpectedText = "$prefix($projectDependency)"

        val fileContents = tipOfTreeBuildFile.asFile.get().readText()
        val foundExpectedText = fileContents.contains(fullExpectedText)

        // Optional projects use a different path for docs configuration
        val foundOptionalText =
            includeOptionalProjects.get() &&
                fileContents.contains("${prefix}ForOptionalProject(\"$projectPath\")")

        if (!foundExpectedText && !foundOptionalText) {
            // If this is a KMP project, check if it is present but configured as non-KMP
            val message =
                if (fileContents.contains(projectDependency)) {
                    "Project $projectPath has the wrong configuration type in " +
                        "docs-tip-of-tree/build.gradle, should use $prefix\n\n" +
                        "Update the entry for $projectPath in docs-tip-of-tree/build.gradle to " +
                        "'$fullExpectedText'."
                } else {
                    "Project $projectPath not found in docs-tip-of-tree/build.gradle\n\n" +
                        "Use the project creation script (development/project-creator/" +
                        "create_project.py) when setting up a project to make sure all required " +
                        "steps are complete.\n\n" +
                        "The project should be added to docs-tip-of-tree/build.gradle as " +
                        "\'$fullExpectedText\'.\n\n" +
                        "If this project should not have published refdocs, first check that the " +
                        "library type listed in its build.gradle file is accurate. If it is, opt out " +
                        "of refdoc generation using \'doNotDocumentReason = \"some reason\"\' in the " +
                        "'androidx' configuration section (this is not common)."
                }
            throw GradleException(message)
        }
    }

    companion object {
        fun Project.setUpCheckDocsTask(extension: AndroidXExtension) {
            project.afterEvaluate {
                if (!extension.requiresDocs()) return@afterEvaluate

                val docsType =
                    if (extension.type == LibraryType.Companion.SAMPLES) {
                        DocsType.SAMPLES
                    } else if (multiplatformExtension != null) {
                        DocsType.KMP
                    } else {
                        DocsType.STANDARD
                    }

                val checkDocs =
                    project.tasks.register(
                        "checkDocsTipOfTree",
                        CheckTipOfTreeDocsTask::class.java
                    ) { task ->
                        task.tipOfTreeBuildFile.set(
                            project.getSupportRootFolder().resolve("docs-tip-of-tree/build.gradle")
                        )
                        task.projectPathProvider.set(path)
                        task.includeOptionalProjects.set(
                            providers
                                .gradleProperty(INCLUDE_OPTIONAL_PROJECTS)
                                .getOrElse("false")
                                .toBoolean()
                        )
                        task.type.set(docsType)
                        task.cacheEvenIfNoOutputs()
                    }
                project.addToBuildOnServer(checkDocs)
            }
        }

        enum class DocsType(val prefix: String) {
            STANDARD("docs"),
            KMP("kmpDocs"),
            SAMPLES("samples"),
        }

        /**
         * Whether the project should have public docs. True for API-tracked projects and samples,
         * unless opted-out with [AndroidXExtension.doNotDocumentReason]
         */
        fun AndroidXExtension.requiresDocs() =
            shouldConfigureApiTasks() && doNotDocumentReason == null
    }
}
