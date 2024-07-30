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

import org.gradle.api.Project
import org.gradle.api.artifacts.component.ModuleComponentSelector

/**
 * If useMaxDepVersions is set, iterate through all the dependencies and substitute any androidx
 * artifact dependency with the local tip of tree version of the library.
 */
internal fun Project.configureMaxDepVersions(extension: AndroidXExtension) {
    if (!usingMaxDepVersions()) return
    // TODO(153485458) remove most of these exceptions
    if (name.contains("hilt") || name == "camera-testapp-timing" || name == "room-testapp") {
        return
    }
    val projectModules = extension.mavenCoordinatesToProjectPathMap
    configurations.configureEach { configuration ->
        configuration.resolutionStrategy.dependencySubstitution.apply {
            all { dep ->
                val requested = dep.requested
                if (requested is ModuleComponentSelector) {
                    val module = requested.group + ":" + requested.module
                    if (
                        // todo(b/331800231): remove compiler exception.
                        requested.group != "androidx.compose.compiler" &&
                            projectModules.containsKey(module)
                    ) {
                        dep.useTarget(project(projectModules[module]!!))
                    }
                }
            }
        }
    }
}
