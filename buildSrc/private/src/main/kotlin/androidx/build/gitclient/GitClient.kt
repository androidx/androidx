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

import androidx.build.gitclient.GitHeadShaSource.Parameters
import java.io.ByteArrayOutputStream
import java.io.File
import java.nio.charset.Charset
import javax.inject.Inject
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.provider.ValueSource
import org.gradle.api.provider.ValueSourceParameters
import org.gradle.process.ExecOperations

/**
 * @param baseCommitOverride optional value to use to override last merge commit
 * @return provider that has the changes files since the last merge commit. It will use CHANGE_INFO
 *   and MANIFEST to resolve the files if these environmental variables are set, otherwise it will
 *   default to using git.
 */
fun Project.getChangedFilesProvider(
    baseCommitOverride: Provider<String>,
): Provider<List<String>> {
    val changeInfoPath = System.getenv("CHANGE_INFO")
    val manifestPath = System.getenv("MANIFEST")
    return if (changeInfoPath != null && manifestPath != null) {
        if (baseCommitOverride.isPresent())
            throw GradleException(
                "Overriding base commit is not supported when using CHANGE_INFO and MANIFEST"
            )
        getChangedFilesFromChangeInfoProvider(manifestPath, changeInfoPath)
    } else if (changeInfoPath != null) {
        throw GradleException("Setting CHANGE_INFO requires also setting MANIFEST")
    } else if (manifestPath != null) {
        throw GradleException("Setting MANIFEST requires also setting CHANGE_INFO")
    } else {
        providers.of(GitChangedFilesSource::class.java) {
            it.parameters.workingDir.set(rootProject.layout.projectDirectory)
            it.parameters.baseCommitOverride.set(baseCommitOverride)
        }
    }
}

/**
 * @return provider of HEAD SHA. It will use MANIFEST to get the SHA if the environmental variable
 *   is set, otherwise it will default to using git.
 */
fun getHeadShaProvider(project: Project): Provider<String> {
    val manifestPath = System.getenv("MANIFEST")
    return if (manifestPath != null) { // using manifest xml file for HEAD SHA
        project.getHeadShaFromManifestProvider(manifestPath)
    } else { // using git for HEAD SHA
        project.providers.of(GitHeadShaSource::class.java) {
            it.parameters.workingDir.set(project.layout.projectDirectory)
        }
    }
}

/** Provides HEAD SHA by calling git in [Parameters.workingDir]. */
internal abstract class GitHeadShaSource : ValueSource<String, GitHeadShaSource.Parameters> {
    interface Parameters : ValueSourceParameters {
        val workingDir: DirectoryProperty
    }

    @get:Inject abstract val execOperations: ExecOperations

    override fun obtain(): String {
        val output = ByteArrayOutputStream()
        execOperations.exec {
            it.commandLine("git", "rev-parse", "HEAD")
            it.standardOutput = output
            it.workingDir = findGitDirInParentFilepath(parameters.workingDir.get().asFile)
        }
        return String(output.toByteArray(), Charset.defaultCharset()).trim()
    }
}

/** Provides changed files since the last merge by calling git in [Parameters.workingDir]. */
internal abstract class GitChangedFilesSource :
    ValueSource<List<String>, GitChangedFilesSource.Parameters> {
    interface Parameters : ValueSourceParameters {
        val workingDir: DirectoryProperty
        val baseCommitOverride: Property<String?>
    }

    @get:Inject abstract val execOperations: ExecOperations

    override fun obtain(): List<String> {
        val output = ByteArrayOutputStream()
        val gitDirInParentFilepath = findGitDirInParentFilepath(parameters.workingDir.get().asFile)
        val baseCommit =
            if (parameters.baseCommitOverride.isPresent) {
                parameters.baseCommitOverride.get()
            } else {
                // Call git to get the last merge commit
                execOperations.exec {
                    it.commandLine(
                        "git",
                        "log",
                        "-1",
                        "--merges",
                        "--oneline",
                        "--pretty=format:%H"
                    )
                    it.standardOutput = output
                    it.workingDir = gitDirInParentFilepath
                }
                String(output.toByteArray(), Charset.defaultCharset()).trim()
            }
        // Get the list of changed files since the last git merge commit
        execOperations.exec {
            it.commandLine("git", "diff", "--name-only", "HEAD", baseCommit)
            it.standardOutput = output
            it.workingDir = gitDirInParentFilepath
        }
        return String(output.toByteArray(), Charset.defaultCharset())
            .split(System.lineSeparator())
            .filterNot { it.isEmpty() }
    }
}

/** Finds the git directory containing the given File by checking parent directories */
private fun findGitDirInParentFilepath(filepath: File): File? {
    var curDirectory: File = filepath
    while (curDirectory.path != "/") {
        if (File("$curDirectory/.git").exists()) {
            return curDirectory
        }
        curDirectory = curDirectory.parentFile
    }
    return null
}
