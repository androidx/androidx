/*
 * Copyright (C) 2018 The Android Open Source Project
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

import androidx.build.pinneddependencies.DependencyExemption
import androidx.build.pinneddependencies.LibraryCoordinates
import androidx.build.pinneddependencies.findVerificationErrors
import androidx.build.uptodatedness.cacheEvenIfNoOutputs
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.artifacts.Dependency
import org.gradle.api.artifacts.ExternalModuleDependency
import org.gradle.api.artifacts.ProjectDependency
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.provider.Provider
import org.gradle.api.provider.SetProperty
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.TaskProvider

/**
 * Task for verifying the androidx dependency-stability-suffix rule (A library is only as stable as
 * its least stable dependency), and that dependencies on libraries that release separately are
 * pinned to a non-alpha released version or listed in
 * [androidx.build.pinneddependencies.EXEMPTIONS_FILE_NAME].
 */
@CacheableTask
internal abstract class VerifyDependencyVersionsTask : DefaultTask() {

    init {
        group = "Verification"
        description =
            "Task for verifying the androidx dependency-stability-suffix rule and that " +
                "dependencies on separately released libraries are pinned to a non-alpha " +
                "version or exempted"
    }

    @get:Input abstract val library: Property<LibraryCoordinates>

    @get:Input abstract val androidXDependencySet: SetProperty<AndroidXDependency>

    @get:Input abstract val exemptionCandidates: SetProperty<LibraryCoordinates>

    @get:Input abstract val exemptions: ListProperty<DependencyExemption>

    /**
     * Iterate through the dependencies of the project and ensure none of them are of an inferior
     * release. This means that a beta project should not have any alpha dependencies, an rc project
     * should not have any alpha or beta dependencies and a stable version should only depend on
     * other stable versions. Dependencies defined with testCompile and friends along with
     * androidTestImplementation and similar are excluded from this verification.
     */
    @TaskAction
    fun verifyDependencyVersions() {
        androidXDependencySet.get().forEach { dependency -> verifyDependencyVersion(dependency) }
        val exemptionCandidates = exemptionCandidates.get()
        if (exemptionCandidates.isEmpty()) return

        val errors =
            findVerificationErrors(
                library = library.get(),
                dependencies = exemptionCandidates,
                exemptions = exemptions.get(),
            )
        if (errors.isEmpty()) return

        val report = errors.joinToString(separator = "\n\n")
        throw GradleException(report)
    }

    private fun verifyDependencyVersion(dependency: AndroidXDependency) {
        val projectVersion = library.get().version.toString()
        val dependencyVersion = dependency.version
        val projectReleasePhase = releasePhase(projectVersion)
        if (projectReleasePhase < 0) {
            throw GradleException("Project has unexpected release phase $projectVersion")
        }
        val dependencyReleasePhase = releasePhase(dependencyVersion)
        if (dependencyReleasePhase < 0) {
            throw GradleException(
                "Dependency ${dependency.group}:${dependency.name}" +
                    ":${dependency.version} has unexpected release phase $dependencyVersion"
            )
        }
        if (dependencyReleasePhase < projectReleasePhase) {
            throw GradleException(
                "Project with version $projectVersion may " +
                    "not take a dependency on less-stable artifact ${dependency.group}:" +
                    "${dependency.name}:${dependency.version} for configuration " +
                    "${dependency.configurationName}. Dependency versions must be at least as " +
                    "stable as the project version."
            )
        }
    }

    private fun releasePhase(versionString: String): Int {
        // If the version is unspecified then treat as an alpha version. If the depending project's
        // version is unspecified then it won't matter, and if the dependency's version is
        // unspecified then any non alpha project won't be able to depend on it to ensure safety.
        val version =
            if (versionString != AndroidXExtension.DEFAULT_UNSPECIFIED_VERSION) {
                Version.parse(versionString)
            } else {
                return 1
            }
        return when {
            version.isStable() -> 4
            version.isRC() -> 3
            version.isBeta() -> 2
            version.isAlpha() || version.isDev() || version.isPrereleasePrefix("qpreview") -> 1
            else -> -1
        }
    }
}

data class AndroidXDependency(
    val group: String,
    val name: String,
    val version: String,
    val configurationName: String,
) : java.io.Serializable {
    companion object {
        private const val serialVersionUID = 344435634564L
    }
}

internal fun Project.createVerifyDependencyVersionsTask(
    libraryVersionsService: Provider<LibraryVersionsService>
): TaskProvider<VerifyDependencyVersionsTask> {
    val usingMaxDepsVersions = project.usingMaxDepVersions()
    val taskProvider =
        tasks.register("verifyDependencyVersions", VerifyDependencyVersionsTask::class.java) { task
            ->
            val library = getLibraryCoordinates(libraryVersionsService)
            task.library.set(library)
            task.androidXDependencySet.set(
                project.provider {
                    project.configurations
                        .matching { it.isCanBeConsumed }
                        .flatMap { configuration ->
                            configuration.allDependencies.filter(::shouldVerifyDependency).map {
                                dependency ->
                                AndroidXDependency(
                                    dependency.group!!,
                                    dependency.name,
                                    dependency.version!!,
                                    configuration.name,
                                )
                            }
                        }
                }
            )
            task.exemptionCandidates.set(getExemptionCandidates(libraryVersionsService))
            task.exemptions.set(
                library.zip(libraryVersionsService) { lib, service ->
                    service.exemptionsFor(lib.coordinate)
                }
            )

            task.onlyIf {
                /**
                 * Ignore -Pandroidx.useMaxDepVersions when verifying dependency versions because it
                 * is a hypothetical build which is only intended to check for forward
                 * compatibility.
                 */
                !usingMaxDepsVersions.get()
            }
            task.cacheEvenIfNoOutputs()
        }

    addToBuildOnServer(taskProvider)
    return taskProvider
}

private fun shouldVerifyDependency(dependency: Dependency): Boolean {
    // Only verify dependencies within the scope of our versioning policies.
    if (dependency.group == null) return false
    if (!dependency.group!!.startsWith("androidx.")) return false
    if (dependency.version == SNAPSHOT_MARKER) {
        // This only happens in playground builds where this magic version gets replaced with
        // the version from the snapshotBuildId defined in playground-common/playground.properties.
        // It is best to leave their validation to the aosp build to ensure it is the right
        // version.
        return false
    }

    return true
}

internal fun Project.getLibraryCoordinates(
    libraryVersionsService: Provider<LibraryVersionsService>
): Provider<LibraryCoordinates> {
    val projectPath = path
    val projectVersion = Version.parse(version.toString())
    val projectGroup = group.toString()
    val projectArtifact = name
    val mavenVersionGroup = parse().mavenVersionGroup
    return libraryVersionsService.map { service ->
        LibraryCoordinates(
            projectPath = projectPath,
            groupId = projectGroup,
            artifactId = projectArtifact,
            version = projectVersion,
            versionGroup = service.versionGroupFor(projectPath, projectGroup, mavenVersionGroup),
        )
    }
}

internal fun Project.getExemptionCandidates(
    libraryVersionService: Provider<LibraryVersionsService>
): Provider<Set<LibraryCoordinates>> {
    val declaredDependencies =
        project.configurations
            .matching { it.isCanBeConsumed }
            .flatMap { configuration -> configuration.allDependencies }
            .filter(::shouldVerifyDependency)
            .mapNotNull { dependency ->
                when (dependency) {
                    is ProjectDependency ->
                        LibraryCoordinates(
                            projectPath = dependency.path,
                            groupId = dependency.group!!,
                            artifactId = dependency.name,
                            versionGroup =
                                parseBuildFile(project(dependency.path).buildFile)
                                    .mavenVersionGroup,
                        )
                    is ExternalModuleDependency ->
                        LibraryCoordinates(
                            projectPath = null,
                            groupId = dependency.group,
                            artifactId = dependency.name,
                            version = dependency.version?.let { Version.parseOrNull(it) },
                        )
                    else -> null
                }
            }
            .toSet()

    return libraryVersionService.map { service ->
        declaredDependencies
            .map { dependency ->
                dependency.copy(
                    versionGroup =
                        dependency.projectPath?.let {
                            service.versionGroupFor(it, dependency.groupId, dependency.versionGroup)
                        }
                )
            }
            .toSet()
    }
}
