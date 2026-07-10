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

package androidx.build

import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.artifacts.component.ModuleComponentSelector

/**
 * If useMaxDepVersions is set, iterate through all the dependencies and substitute any androidx
 * artifact dependency with the local tip of tree version of the library.
 */
internal fun Project.configureMaxDepVersions(extension: AndroidXExtension) {
    if (!usingMaxDepVersions().get()) return
    val projectModules = extension.mavenCoordinatesToProjectPathMap

    val extraProperties = project.gradle.extensions.extraProperties
    if (!extraProperties.has("includedProjectPaths")) {
        throw GradleException("Cannot read included project paths to set max dep versions")
    }
    // Use includedProjectPaths to avoid trying to avoid substituting project paths outside
    // the currently configured set when using PROJECT_PREFIX or otherwise constraining the set
    // of included projects
    @Suppress("UNCHECKED_CAST")
    val includedProjectPaths = extraProperties["includedProjectPaths"] as Set<String>

    configurations.configureEach { configuration ->
        configuration.resolutionStrategy.dependencySubstitution.apply {
            all { dep ->
                val requested = dep.requested
                if (requested is ModuleComponentSelector) {
                    val module = requested.group + ":" + requested.module
                    val targetPath = projectModules[module]
                    if (targetPath != null && includedProjectPaths.contains(targetPath)) {
                        dep.useTarget(project(targetPath))
                    }
                }
            }
        }
    }
}
