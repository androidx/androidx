/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.build.playground

import androidx.build.dependencyTracker.AffectedModuleDetectorImpl
import androidx.build.gitclient.Commit
import androidx.build.gitclient.GitClient
import androidx.build.gitclient.GitCommitRange
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.options.Option
import java.io.File

/**
 * A task to print the list of affected modules based on given parameters.
 *
 * The list of changed files can be passed via [changedFiles] property and the the list of module
 * paths will be written to the given [outputFilePath].
 *
 * This task is specialized for Playground projects where any change in .github or
 * playground-common will be considered as an `INFRA` change and will be listed in the outputs.
 */
abstract class FindAffectedModulesTask : DefaultTask() {
    @get:Input
    @set:Option(
        option = "changedFilePath",
        description = "Changed file in the build (including removed files). Can be passed " +
            "multiple times, e.g.: --changedFilePath=a.kt --changedFilePath=b.kt " +
            "File paths must be relative to the root directory of the main checkout"
    )
    abstract var changedFiles: List<String>

    @get:Input
    @set:Option(
        option = "outputFilePath",
        description = "The file path where the result of whether this project is affected by " +
            "changes or not is written."
    )
    abstract var outputFilePath: String

    @TaskAction
    fun checkAffectedModules() {
        val hasChangedGithubInfraFiles = changedFiles.any {
            it.contains(".github") ||
                it.contains("playground-common") ||
                it.contains("buildSrc")
                // TODO cover root module
            }
        val detector = AffectedModuleDetectorImpl(
            rootProject = project,
            injectedGitClient = ChangedFilesGitClient(changedFiles),
            logger = logger
        )
        val changedProjectPaths = detector.affectedProjects.map {
            it.path
        } + if (hasChangedGithubInfraFiles) {
            listOf("INFRA")
        } else {
            emptyList()
        }
        val outputFile = File(outputFilePath)
        check(outputFile.parentFile?.exists() == true) {
            "invalid output file argument: $outputFile. Make sure to pass an absolute path"
        }
        val changedProjects = changedProjectPaths.joinToString(System.lineSeparator())
        outputFile.writeText(changedProjects, charset = Charsets.UTF_8)
        logger.info("putting result $changedProjects into ${outputFile.absolutePath}")
    }

    /**
     * GitClient implementation that just returns the files that were passed in as changed files
     */
    private class ChangedFilesGitClient(
        val changedFiles: List<String>
    ) : GitClient {
        override fun findChangedFilesSince(
            sha: String,
            top: String,
            includeUncommitted: Boolean
        ): List<String> = changedFiles

        override fun findPreviousSubmittedChange(): String = "ignored"

        override fun getGitLog(
            gitCommitRange: GitCommitRange,
            keepMerges: Boolean,
            fullProjectDir: File
        ): List<Commit> = emptyList()
    }
}