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

package org.jetbrains.androidx.build

import androidx.build.AndroidXDependency
import androidx.build.Version
import androidx.build.multiplatformExtension
import androidx.build.uptodatedness.cacheEvenIfNoOutputs
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.provider.Property
import org.gradle.api.provider.SetProperty
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction
import org.gradle.kotlin.dsl.setProperty

/**
 * Task for verifying the library dependency-stability-suffix rule (A library is only as stable as
 * its least stable dependency)
 */
@CacheableTask
abstract class JetBrainsVerifyDependencyVersionsTask : DefaultTask() {

    init {
        group = "Compose Multiplatform"
        description = "Task for verifying the library dependency-stability-suffix rule"
    }

    @get:Input
    abstract val version: Property<String>

    @get:Input
    val androidXDependencySet: SetProperty<AndroidXDependency> = project.objects.setProperty()

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
    }

    private fun verifyDependencyVersion(dependency: AndroidXDependency) {
        val projectVersionExtra = Version(version.get()).extra ?: ""
        val dependencyVersionExtra = Version(dependency.version).extra ?: ""
        val projectReleasePhase = releasePhase(projectVersionExtra)
        if (projectReleasePhase < 0) {
            throw GradleException("Project has unexpected release phase $projectVersionExtra")
        }
        val dependencyReleasePhase = releasePhase(dependencyVersionExtra)
        if (dependencyReleasePhase < 0) {
            throw GradleException(
                "Dependency ${dependency.group}:${dependency.name}" +
                    ":${dependency.version} has unexpected release phase $dependencyVersionExtra"
            )
        }
        if (dependencyReleasePhase < projectReleasePhase) {
            throw GradleException(
                "Project with version ${version.get()} may " +
                    "not take a dependency on less-stable artifact ${dependency.group}:" +
                    "${dependency.name}:${dependency.version} for configuration " +
                    "${dependency.configurationName}. Dependency versions must be at least as " +
                    "stable as the project version."
            )
        }
    }

    private fun releasePhase(versionExtra: String): Int {
        return when {
            versionExtra == "" -> 4
            versionExtra.startsWith("-rc") -> 3
            versionExtra.startsWith("-beta") -> 2
            versionExtra.startsWith("-alpha") -> 1
            versionExtra.startsWith("-SNAPSHOT") -> 0
            else -> -1
        }
    }
}

internal fun Project.configureDependencyVerification() {
    // Verify only what is publishing
    val component = JetBrainsPublication.projectPathToComponent[path] ?: return

    tasks.register(
        "jbVerifyDependencyVersions",
        JetBrainsVerifyDependencyVersionsTask::class.java
    ) { task ->
        task.version.set(project.provider { project.version.toString() })
        task.androidXDependencySet.set(
            project.provider {
                multiplatformExtension!!
                    .targets
                    .filter { target ->
                        component.supportedPlatforms.any {
                            it.matches(target.name) && !hasRedirection(it)
                        }
                    }
                    .flatMap { target ->
                        target.compilations
                            .filter { !it.name.contains("test", ignoreCase = true) }
                            .flatMap { compilation ->
                                listOf(
                                    compilation.implementationConfigurationName,
                                    compilation.apiConfigurationName,
                                    compilation.runtimeOnlyConfigurationName
                                )
                            }
                    }
                    .asSequence()
                    .map { project.configurations.getByName(it) }
                    .flatMap { configuration ->
                        configuration.allDependencies
                            .filter { it.group != null && it.version != null }
                            .map { dependency ->
                                AndroidXDependency(
                                    dependency.group!!,
                                    dependency.name,
                                    dependency.version!!,
                                    configuration.name,
                                )
                            }
                    }
                    .toList()
            }
        )
        task.cacheEvenIfNoOutputs()
    }
}
