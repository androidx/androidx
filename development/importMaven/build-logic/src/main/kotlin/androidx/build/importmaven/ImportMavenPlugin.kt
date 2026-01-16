/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.build.importmaven

import java.io.File
import okio.FileSystem
import okio.Path
import okio.Path.Companion.toOkioPath
import org.gradle.api.Plugin
import org.gradle.api.Project

class ImportMavenPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        project.tasks.register("import")
        project.defaultTasks("import")
        if (
            !project.providers.gradleProperty("artifacts").isPresent &&
                !project.providers.gradleProperty("importToml").isPresent
        ) {
            println("Expected either -Partifacts=foo:bar:1.0.0 or -PimportToml to be used.")
            return
        }

        val extraRepositories = mutableListOf<String>()
        project.providers.gradleProperty("androidxBuildId").orNull?.let {
            extraRepositories.add(ArtifactResolver.createAndroidXRepo(it.toInt()))
        }
        project.providers.gradleProperty("metalavaBuildId").orNull?.let {
            extraRepositories.add(ArtifactResolver.createMetalavaRepo(it.toInt()))
        }
        if (project.providers.gradleProperty("allowJetbrainsDev").isPresent) {
            extraRepositories.addAll(ArtifactResolver.jetbrainsRepositories)
        }
        val redownload = project.providers.gradleProperty("redownload").isPresent

        val supportDirectory = File(project.rootDir, "../../").toOkioPath()
        val downloadDirectory = supportDirectory / "../../prebuilts/androidx/"
        val downloader =
            LocalMavenRepoDownloader(
                fileSystem = FileSystem.SYSTEM,
                internalFolder = downloadDirectory / "internal",
                externalFolder = downloadDirectory / "external",
            )

        val artifactsToBeResolved =
            if (project.providers.gradleProperty("importToml").isPresent) {
                ImportVersionCatalog.load(project)
            } else {
                project.providers.gradleProperty("artifacts").get().split(",").filterNot { it.isBlank() }
            }
        println("Artifacts: ")
        artifactsToBeResolved.forEach { println(it) }
        println("Prebuilts: ")
        println(
            downloader.internalFolder.normalized().toString() +
                "\n" +
                downloader.externalFolder.normalized().toString()
        )

        val result =
            ArtifactResolver.resolveArtifacts(
                artifacts = artifactsToBeResolved,
                additionalRepositories = extraRepositories,
                explicitlyFetchInheritedDependencies = false,
                localRepositories =
                    if (redownload) {
                        emptyList()
                    } else {
                        listOf(
                            "file:///" + downloader.internalFolder.normalized().toString(),
                            "file:///" + downloader.externalFolder.normalized().toString(),
                        )
                    },
                downloadObserver = downloader,
            )
        val resolvedArtifacts = result.artifacts

        val downloadedFiles = downloader.getDownloadedFiles()
        println(
            """
                --------------------------------------------------------------------------------
                Resolved ${resolvedArtifacts.size} artifacts.
                Downloaded ${downloadedFiles.size} new files.
                --------------------------------------------------------------------------------
            """
                .trimIndent()
        )
        updatePlaygroundMetalavaBuildIfNecessary(
            downloadedFiles,
            project.providers.gradleProperty("metalavaBuildId").orNull,
            supportDirectory,
        )
        if (downloadedFiles.isEmpty()) {
            println(
                """
                [31mDidn't download any files. It might be either a bug or all files might be
                available in the local prebuilts.

                If you think it is a bug, please re-run the command with `--verbose` and file
                a bug with the output.
                https://issuetracker.google.com/issues/new?component=705292[0m
                """
                    .trimIndent()
            )
        } else {
            if (!result.dependenciesPassedVerification) {
                println(
                    """
                   [33mOur Gradle build won't trust any artifacts that are unsigned or are signed with new keys.
                   To trust these artifacts, you might need run `development/update-verification-metadata.sh`
                   later if Gradle's dependency verification fails when you run a Gradle command. [0m
                   """
                        .trimIndent()
                )
            }
        }
    }

    /**
     * GitHub Playground's metalava build id needs to match the build id used by androidx.
     *
     * This method takes care of updating playground.properties if the metalava build id is
     * specified in the import maven script, and we've downloaded metalava.
     */
    private fun updatePlaygroundMetalavaBuildIfNecessary(
        downloadedFiles: Set<Path>,
        metalavaBuildId: String?,
        supportDirectory: Path,
    ) {
        val metalavaBuild = metalavaBuildId ?: return
        val downloadedMetalava = downloadedFiles.any { it.name.contains("metalava") }
        if (!downloadedMetalava) {
            return
        }
        val playgroundPropertiesFile =
            (supportDirectory / "playground-common/playground.properties").toFile()
        check(playgroundPropertiesFile.exists()) {
            """
                Cannot find playground properties file. This is needed to update metalava in
                playground to match AndroidX.
            """
                .trimIndent()
        }
        val updatedProperties =
            playgroundPropertiesFile.readLines(Charsets.UTF_8).joinToString("\n") {
                if (it.trim().startsWith("androidx.playground.metalavaBuildId=")) {
                    "androidx.playground.metalavaBuildId=$metalavaBuild"
                } else {
                    it
                }
            }
        playgroundPropertiesFile.writeText(updatedProperties)
        println("updated playground properties")
    }
}
