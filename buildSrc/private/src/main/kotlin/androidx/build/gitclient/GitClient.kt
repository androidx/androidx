/*
 * Copyright 2018 The Android Open Source Project
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

package androidx.build.gitclient

import androidx.build.getCheckoutRoot
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.logging.Logger
import org.gradle.api.provider.Provider

import org.gradle.api.provider.Property
import org.gradle.api.file.DirectoryProperty
import java.io.ByteArrayOutputStream
import javax.inject.Inject
import org.gradle.api.provider.ValueSource
import org.gradle.api.provider.ValueSourceParameters
import org.gradle.process.ExecOperations

interface GitClient {
    fun findChangedFilesSince(sha: String): List<String>

    fun findPreviousSubmittedChange(): String?

    /** Returns the full commit sha for the HEAD of the git repository */
    fun getHeadSha(): String

    /** Abstraction for running execution commands for testability */
    interface CommandRunner {
        /** Executes the given shell command and returns the stdout as a string. */
        fun execute(command: String): String
        /** Executes the given shell command and returns the stdout by lines. */
        fun executeAndParse(command: String): List<String>
    }

    companion object {
        fun getChangeInfoPath(project: Project): Provider<String> {
            return project.providers.environmentVariable("CHANGE_INFO").orElse("")
        }

        fun getManifestPath(project: Project): Provider<String> {
            return project.providers.environmentVariable("MANIFEST").orElse("")
        }

        fun forProject(project: Project): GitClient {
            return create(
                project.projectDir,
                project.getCheckoutRoot(),
                project.logger,
                GitClient.getChangeInfoPath(project).get(),
                GitClient.getManifestPath(project).get(),
                project.getGitLog(project.objects.directoryProperty().convention(project.layout.projectDirectory))
            )
        }

        fun create(
            projectDir: File,
            checkoutRoot: File,
            logger: Logger,
            changeInfoPath: String,
            manifestPath: String,
            getGitLog: Provider<String>
        ): GitClient {
            if (changeInfoPath != "") {
                if (manifestPath == "") {
                    throw GradleException("Setting CHANGE_INFO requires also setting MANIFEST")
                }
                val changeInfoFile = File(changeInfoPath)
                val manifestFile = File(manifestPath)
                if (!changeInfoFile.exists()) {
                    throw GradleException("changeinfo file $changeInfoFile does not exist")
                }
                if (!manifestFile.exists()) {
                    throw GradleException("manifest $manifestFile does not exist")
                }
                val changeInfoText = changeInfoFile.readText()
                val manifestText = manifestFile.readText()
                val projectDirRelativeToRoot = projectDir.relativeTo(checkoutRoot).toString()
                logger.info(
                    "Using ChangeInfoGitClient with change info path $changeInfoPath, " +
                        "manifest $manifestPath project dir $projectDirRelativeToRoot"
                )
                return ChangeInfoGitClient(changeInfoText, manifestText, projectDirRelativeToRoot)
            }
            val gitRoot = findGitDirInParentFilepath(projectDir)
            check(gitRoot != null) { "Could not find .git dir for $projectDir" }
            logger.info("UsingGitRunnerGitClient")
            return GitRunnerGitClient(gitRoot, logger, getGitLog)
        }
    }
}

data class MultiGitClient(
    val checkoutRoot: File,
    val logger: Logger,
    val changeInfoPath: String,
    val manifestPath: String,
    val getGitLog: Provider<String>
) {
    // Map from the root of the git repository to a GitClient for that repository
    // In AndroidX this directory could be frameworks/support, external/noto-fonts, or others
    @Transient // We don't want Gradle to persist GitClient in the configuration cache
    var cache: MutableMap<File, GitClient>? = null

    fun getGitClient(projectDir: File): GitClient {
        // If this object was restored from the Configuration cache, this value will be null
        // So, if it is null we have to reinitialize it
        var cache = this.cache
        if (cache == null) {
            cache = ConcurrentHashMap()
            this.cache = cache
        }
        return cache.getOrPut(key = projectDir) {
            GitClient.create(projectDir, checkoutRoot, logger, changeInfoPath, manifestPath, getGitLog)
        }
    }

    companion object {
        fun create(project: Project): MultiGitClient {
            return MultiGitClient(
                project.getCheckoutRoot(),
                project.logger,
                GitClient.getChangeInfoPath(project).get(),
                GitClient.getManifestPath(project).get(),
                project.getGitLog(project.objects.directoryProperty().convention(project.layout.projectDirectory))
            )
        }
    }
}

fun Project.getGitLog(workingDir: DirectoryProperty): Provider<String> {
    return providers.of(GitLogValueSource::class.java) {
        it.parameters.workingDir.set(workingDir)
    }
}

abstract class GitLogValueSource : ValueSource<String, GitLogValueSource.Parameters> {

    interface Parameters : ValueSourceParameters {
        val workingDir: DirectoryProperty
    }

    @get:Inject
    abstract val execOperations: ExecOperations

    internal fun findGitDirInParentFilepath(filepath: File): File? {
        var curDirectory: File = filepath
        while (curDirectory.path != "/") {
            if (File("$curDirectory/.git").exists()) {
                return curDirectory
            }
            curDirectory = curDirectory.parentFile
        }
        return null
    }

    override fun obtain(): String {
        val output = ByteArrayOutputStream()
        execOperations.exec {
            it.commandLine("git", "log", "--name-only", "--pretty=format:%H", "HEAD", "-n", "1", "--" ,"./")
            it.standardOutput = output
            it.workingDir = findGitDirInParentFilepath(parameters.workingDir.get().asFile)
        }
        return String(output.toByteArray(), java.nio.charset.Charset.defaultCharset())
    }
}
