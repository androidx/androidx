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

import androidx.build.dependencyTracker.AffectedModuleDetector
import androidx.build.dependencyTracker.AffectedModuleDetectorImpl
import androidx.build.gitclient.Commit
import androidx.build.gitclient.GitClient
import androidx.build.gitclient.GitCommitRange
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.options.Option
import java.io.File

abstract class CheckAffectedPlaygroundTask : DefaultTask() {
    @get:Input
    @set:Option(
        option = "changedFile",
        description = "Changed file in the build (including removed files). Can be passed " +
            "multiple times, e.g.: --changedFile=a.kt --changedFile=b.kt "
    )
    abstract var changedFiles: List<String>

    @TaskAction
    fun checkAffectedModules() {
        val hasChangedGithubInfraFiles = changedFiles.any {
            it.startsWith(".github") ||
                it.startsWith("playground-common")
            }
        val detector = AffectedModuleDetectorImpl(
            rootProject = project,
            injectedGitClient = ChangedFilesGitClient(changedFiles),
            logger = logger
        )
        println("changed files:")
        detector.affectedProjects.forEach { module ->
            println(module.path)
        }
        println("end of changed files")
    }

    private class ChangedFilesGitClient(
        val changedFiles: List<String>
    ) : GitClient {
        override fun findChangedFilesSince(
            sha: String,
            top: String,
            includeUncommitted: Boolean
        ): List<String> = changedFiles

        override fun findPreviousSubmittedChange(): String? = "ignored"

        override fun getGitLog(
            gitCommitRange: GitCommitRange,
            keepMerges: Boolean,
            fullProjectDir: File
        ): List<Commit> = emptyList()

    }
}