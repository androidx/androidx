/*
 * Copyright 2020 The Android Open Source Project
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

package androidx.build

import androidx.build.gradle.extraPropertyOrNull
import androidx.build.gradle.isRoot
import groovy.xml.DOMBuilder
import java.net.URI
import java.net.URL
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.dsl.RepositoryHandler
import org.gradle.api.tasks.testing.AbstractTestTask
import org.gradle.work.DisableCachingByDefault

/**
 * This plugin is used in Playground projects and adds functionality like resolving to snapshot
 * artifacts instead of projects or allowing access to public maven repositories.
 */
@Suppress("unused") // used in Playground Projects
class AndroidXPlaygroundRootImplPlugin : Plugin<Project> {
    private lateinit var rootProject: Project

    /** List of snapshot repositories to fetch AndroidX artifacts */
    private lateinit var repos: PlaygroundRepositories

    /** The configuration for the plugin read from the gradle properties */
    private lateinit var config: PlaygroundProperties

    /** List of projects that were requested in the settings.gradle file */
    private lateinit var primaryProjectPaths: Set<String>

    override fun apply(target: Project) {
        if (!target.isRoot) {
            throw GradleException("This plugin should only be applied to root project")
        }
        if (!target.plugins.hasPlugin(AndroidXRootImplPlugin::class.java)) {
            throw GradleException(
                "Must apply AndroidXRootImplPlugin before applying AndroidXPlaygroundRootImplPlugin"
            )
        }
        rootProject = target
        config = PlaygroundProperties.load(rootProject)
        repos = PlaygroundRepositories(config)
        rootProject.repositories.addPlaygroundRepositories()
        GradleTransformWorkaround.maybeApply(rootProject)
        PlaygroundCIHostTestsTask.register(rootProject)
        primaryProjectPaths =
            target.extensions.extraProperties.get("primaryProjects")!!.toString().split(",").toSet()
        rootProject.subprojects { configureSubProject(it) }
    }

    private fun configureSubProject(project: Project) {
        project.repositories.addPlaygroundRepositories()
        project.configurations.configureEach { configuration ->
            configuration.resolutionStrategy.eachDependency { details ->
                val requested = details.requested
                if (requested.version == SNAPSHOT_MARKER) {
                    val snapshotVersion = findSnapshotVersion(requested.group, requested.name)
                    details.useVersion(snapshotVersion)
                }
            }
        }
        if (project.path in primaryProjectPaths) {
            project.tasks.withType(AbstractTestTask::class.java).configureEach {
                PlaygroundCIHostTestsTask.addTask(project, it)
            }
        }
    }

    /**
     * Finds the snapshot version from the AndroidX snapshot repository.
     *
     * This is initially done by reading the maven-metadata from the snapshot repository. The result
     * of that query is cached in the build file so that subsequent build requests will not need to
     * access the network.
     */
    private fun findSnapshotVersion(group: String, module: String): String {
        @Suppress("DEPRECATION")
        val snapshotVersionCache =
            rootProject.buildDir.resolve("snapshot-version-cache/${config.snapshotBuildId}")
        val groupPath = group.replace('.', '/')
        val modulePath = module.replace('.', '/')
        val metadataCacheFile = snapshotVersionCache.resolve("$groupPath/$modulePath/version.txt")
        return if (metadataCacheFile.exists()) {
            metadataCacheFile.readText(Charsets.UTF_8)
        } else {
            val metadataUrl = "${repos.snapshots.url}/$groupPath/$modulePath/maven-metadata.xml"
            @Suppress("deprecation")
            URL(metadataUrl).openStream().use {
                val parsedMetadata = DOMBuilder.parse(it.reader())
                val versionNodes = parsedMetadata.getElementsByTagName("latest")
                if (versionNodes.length != 1) {
                    throw GradleException(
                        "AndroidXPlaygroundRootImplPlugin#findSnapshotVersion expected exactly " +
                            " one latest version in $metadataUrl, but got ${versionNodes.length}"
                    )
                }
                val snapshotVersion = versionNodes.item(0).textContent
                metadataCacheFile.parentFile.mkdirs()
                metadataCacheFile.writeText(snapshotVersion, Charsets.UTF_8)
                snapshotVersion
            }
        }
    }

    private fun RepositoryHandler.addPlaygroundRepositories() {
        repos.all.forEach { playgroundRepository ->
            maven { repository ->
                repository.url = URI(playgroundRepository.url)
                repository.metadataSources {
                    it.mavenPom()
                    it.artifact()
                }
                repository.content {
                    it.includeGroupByRegex(playgroundRepository.includeGroupRegex)
                    if (playgroundRepository.includeModuleRegex != null) {
                        it.includeModuleByRegex(
                            playgroundRepository.includeGroupRegex,
                            playgroundRepository.includeModuleRegex,
                        )
                    }
                }
            }
        }
        google { repository ->
            repository.content {
                it.includeGroupByRegex("androidx.*")
                it.includeGroupByRegex("com\\.android.*")
                it.includeGroupByRegex("com\\.google.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }

    private class PlaygroundRepositories(props: PlaygroundProperties) {
        val snapshots =
            PlaygroundRepository(
                "https://androidx.dev/snapshots/builds/${props.snapshotBuildId}/artifacts" +
                    "/repository",
                includeGroupRegex = """androidx\..*""",
            )
        val metalava =
            PlaygroundRepository(
                "https://androidx.dev/metalava/builds/${props.metalavaBuildId}/artifacts" +
                    "/repo/m2repository",
                includeGroupRegex = """com\.android\.tools\.metalava""",
            )
        val prebuilts =
            PlaygroundRepository(
                INTERNAL_PREBUILTS_REPO_URL,
                includeGroupRegex = """androidx\..*""",
            )
        val dokka =
            PlaygroundRepository(
                "https://packages.jetbrains.team/maven/p/kt/dokka-dev",
                includeGroupRegex = """org\.jetbrains\.dokka""",
            )
        val kotlinDev =
            PlaygroundRepository(
                "https://packages.jetbrains.team/maven/p/kt/dev/",
                includeGroupRegex = """org\.jetbrains\.kotlin.*""",
            )
        val all = listOf(snapshots, metalava, dokka, prebuilts, kotlinDev)
    }

    private data class PlaygroundRepository(
        val url: String,
        val includeGroupRegex: String,
        val includeModuleRegex: String? = null,
    )

    private data class PlaygroundProperties(
        val snapshotBuildId: String,
        val metalavaBuildId: String,
    ) {
        companion object {
            fun load(project: Project): PlaygroundProperties {
                return PlaygroundProperties(
                    snapshotBuildId = project.requireProperty(PLAYGROUND_SNAPSHOT_BUILD_ID),
                    metalavaBuildId = project.requireProperty(PLAYGROUND_METALAVA_BUILD_ID),
                )
            }

            private fun Project.requireProperty(name: String): String {
                return checkNotNull(extraPropertyOrNull(name)) {
                        "missing $name property. It must be defined in the gradle.properties file"
                    }
                    .toString()
            }
        }
    }

    companion object {
        const val INTERNAL_PREBUILTS_REPO_URL =
            "https://androidx.dev/storage/prebuilts/androidx/internal/repository"
    }

    @DisableCachingByDefault(because = "This is an anchor task that does no work.")
    abstract class PlaygroundCIHostTestsTask : DefaultTask() {
        init {
            group = "Verification"
            description =
                "Runs host tests that belong to the projects which were explicitly " +
                    "requested in the playground setup."
        }

        companion object {
            private const val NAME = "playgroundCIHostTests"

            fun addTask(project: Project, task: AbstractTestTask) {
                project.rootProject.tasks.named(NAME).configure { it.dependsOn(task) }
            }

            fun register(project: Project) {
                project.tasks.register(NAME, PlaygroundCIHostTestsTask::class.java)
            }
        }
    }
}
