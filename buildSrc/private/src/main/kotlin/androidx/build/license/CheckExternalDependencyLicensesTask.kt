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
package androidx.build.license

import androidx.build.capitalize
import androidx.build.getPrebuiltsRoot
import androidx.build.getVersionByName
import androidx.build.kotlinExtensionOrNull
import java.io.File
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.ExternalDependency
import org.gradle.api.artifacts.component.ModuleComponentIdentifier
import org.gradle.api.artifacts.repositories.MavenArtifactRepository
import org.gradle.api.artifacts.result.ResolvedArtifactResult
import org.gradle.api.attributes.Attribute
import org.gradle.api.attributes.plugin.GradlePluginApiVersion
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.FileCollection
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.kotlin.dsl.named
import org.gradle.util.GradleVersion
import org.gradle.work.DisableCachingByDefault
import org.jetbrains.kotlin.buildtools.api.ExperimentalBuildToolsApi
import org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi

/**
 * This task creates a configuration for the project that has all of its external dependencies and
 * then ensures that those dependencies: a) come from prebuilts b) has a license file.
 */
@DisableCachingByDefault(because = "Too many inputs to declare")
abstract class CheckExternalDependencyLicensesTask : DefaultTask() {
    @get:Input abstract val prebuiltsRoot: Property<String>

    @get:[InputFiles PathSensitive(PathSensitivity.ABSOLUTE)]
    abstract val filesToCheck: ConfigurableFileCollection

    @TaskAction
    fun checkDependencies() {
        val prebuiltsRoot = File(prebuiltsRoot.get())
        val dependencyArtifacts = filesToCheck
        val missingLicenses =
            dependencyArtifacts
                .filter { findLicenseFile(it.canonicalFile, prebuiltsRoot) == null }
                .files
        if (missingLicenses.isNotEmpty()) {
            val suggestions =
                missingLicenses.joinToString("\n") {
                    "$it does not have a license file. It should probably live in " +
                        "${it.parentFile.parentFile}"
                }
            throw GradleException(
                """
                Any external library referenced in the support library
                build must have a LICENSE or NOTICE file next to it in the prebuilts.
                The following libraries are missing it:
                $suggestions
                """
                    .trimIndent()
            )
        }
    }

    private fun findLicenseFile(dependency: File, prebuiltsRoot: File): File? {
        check(dependency.absolutePath.startsWith(prebuiltsRoot.absolutePath)) {
            "Prebuilt file is not part of the prebuilts folder. ${dependency.absoluteFile}"
        }
        fun recurse(folder: File): File? {
            if (folder == prebuiltsRoot) {
                return null
            }
            if (!folder.isDirectory) {
                return recurse(folder.parentFile)
            }

            val found =
                folder.listFiles()!!.firstOrNull {
                    it.name.startsWith("NOTICE", ignoreCase = true) ||
                        it.name.startsWith("LICENSE", ignoreCase = true)
                }
            return found ?: recurse(folder.parentFile)
        }
        return recurse(dependency)
    }

    companion object {
        const val TASK_NAME = "checkExternalLicenses"
    }
}

fun Project.configureExternalDependencyLicenseCheck() {
    tasks.register(
        CheckExternalDependencyLicensesTask.TASK_NAME,
        CheckExternalDependencyLicensesTask::class.java
    ) { task ->
        @OptIn(ExperimentalBuildToolsApi::class, ExperimentalKotlinGradlePluginApi::class)
        val kotlinVersion =
            kotlinExtensionOrNull?.compilerVersion?.get() ?: project.getVersionByName("kotlin")
        task.prebuiltsRoot.set(project.provider { project.getPrebuiltsRoot().absolutePath })
        // configurations.toList() to avoid modify the collection while we iterate over it
        val duplicateConfigs =
            configurations.toList().map { configuration ->
                duplicateForLicenseCheck(configuration, kotlinVersion)
            }
        val localArtifactRepositories = project.findLocalMavenRepositories()
        task.filesToCheck.from(
            duplicateConfigs.flatMap { configuration ->
                configuration.incoming.artifacts.artifacts.mapNotNull {
                    project.validateAndGetArtifactInPrebuilts(it, localArtifactRepositories)
                }
            }
        )
    }
}

/**
 * Checks if the given [ResolvedArtifactResult] resolves to an artifact in prebuilts and if so,
 * returns that File.
 *
 * Note that, when artifacts are published with gradle metadata, the actual resolved file may not
 * reside in prebuilts directory. For those cases, this code re-resolves the artifact from the
 * [repoPaths] and returns the file in prebuilts instead.
 *
 * Returns null if the file does not exist in prebuilts. When it is an error (files outside
 * prebuitls filder is allowed for IDE plugins), throws a [GradleException].
 *
 * @param resolvedArtifact Resolved artifact from the configuration
 * @param repoPaths List of local maven repositories that can be used to resolve the artifact.
 * @return The artifact in prebuilts or null if it does not exist.
 */
private fun Project.validateAndGetArtifactInPrebuilts(
    resolvedArtifact: ResolvedArtifactResult,
    repoPaths: FileCollection
): File? {
    val fileInPrebuilts =
        repoPaths.any { repoPath ->
            resolvedArtifact.file.absolutePath.startsWith(repoPath.absolutePath)
        }
    if (fileInPrebuilts) {
        return resolvedArtifact.file
    }
    // from the artifact coordinates, try to find the actual file in prebuilts.
    // for gradle metadata publishing, resolved file might be moved into .gradle caches
    val id = resolvedArtifact.id.componentIdentifier
    if (id is ModuleComponentIdentifier) {
        // Construct the local folder structure for the artifact to find it in local
        // repositories. If it exists, we'll return that folder instead of the final resolved
        // artifact.
        // For a module: com.google:artifact:1.2.3; the path would be
        // <repo-root>/com/google/artifact/1.2.3
        val subFolder = (id.group.split('.') + id.module + id.version).joinToString(File.separator)
        // if it exists in one of the local repositories, return it.
        repoPaths.forEach {
            val artifactFolder = it.resolve(subFolder)
            if (artifactFolder.exists()) {
                return artifactFolder
            }
        }
    }
    // IDE plugins use dependencies bundled with the IDE itself, so we can ignore this
    // warning for such projects
    if (!project.plugins.hasPlugin("org.jetbrains.intellij")) {
        throw GradleException(
            "prebuilts should come from prebuilts folder. $resolvedArtifact " +
                "(${resolvedArtifact.file} is not there"
        )
    }
    return null
}

/** Returns the list of local maven repository File paths declared in this project. */
private fun Project.findLocalMavenRepositories(): FileCollection {
    val fileList =
        project.repositories
            .mapNotNull {
                if (it is MavenArtifactRepository && it.url.scheme == "file") {
                    it.url
                } else {
                    null
                }
            }
            .map { File(it) }
    return project.files(fileList)
}

private fun Project.duplicateForLicenseCheck(
    configuration: Configuration,
    kotlinVersion: String
): Configuration {
    val duplicate = configurations.create("${configuration.name}${configurationNameSuffix}")
    duplicate.copyAttributesFrom(configuration)
    duplicate.attributes.attribute(
        GradlePluginApiVersion.GRADLE_PLUGIN_API_VERSION_ATTRIBUTE,
        project.objects.named(GradleVersion.current().version)
    )
    val dependencies =
        configuration.dependencies
            .filterIsInstance<ExternalDependency>()
            .filter { it.isValidForLicenseCheck() }
            .onEach { it.fixVersion(kotlinVersion) }
    duplicate.dependencies.addAll(dependencies)
    duplicate.isCanBeConsumed = false
    return duplicate
}

private fun Configuration.copyAttributesFrom(configuration: Configuration) {
    configuration.attributes.keySet().forEach { attrKey ->
        val value: Any = configuration.attributes.getAttribute(attrKey)!!
        @Suppress("UNCHECKED_CAST") attributes.attribute(attrKey as Attribute<Any>, value)
    }
}

private fun ExternalDependency.isValidForLicenseCheck(): Boolean {
    return group?.startsWith("com.android") == false &&
        group?.startsWith("android.arch") == false &&
        group?.startsWith("androidx") == false
}

private fun ExternalDependency.fixVersion(kotlinVersion: String) {
    /* workaround for dependency constraint applied in Kotlin Plugin */
    if (group == "org.jetbrains.kotlin" && name == "kotlin-build-tools-impl") {
        this.version { version -> version.strictly(kotlinVersion) }
    }
}

private val configurationNameSuffix = CheckExternalDependencyLicensesTask.TASK_NAME.capitalize()
