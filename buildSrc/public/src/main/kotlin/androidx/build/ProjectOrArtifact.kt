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

package androidx.build

import org.gradle.api.GradleException
import org.gradle.api.Project

/**
 * Returns a `project` if exists or the latest artifact coordinates if it doesn't.
 *
 * This can be used for optional dependencies in the playground settings.gradle files.
 *
 * @param path The project path
 * @return A Project instance if it exists or coordinates of the artifact if the project is not
 *   included in this build.
 */
fun playgroundProjectOrArtifact(rootProject: Project, path: String): Any {
    val requested = rootProject.findProject(path)
    if (requested != null) {
        return requested
    } else {
        val sections = path.split(":")

        if (sections[0].isNotEmpty()) {
            throw GradleException(
                "Expected projectOrArtifact path to start with empty section but got $path"
            )
        }

        // Typically androidx projects have 3 sections, compose has 4.
        if (sections.size >= 3) {
            val group =
                sections
                    // Filter empty sections as many declarations start with ':'
                    .filter { it.isNotBlank() }
                    // Last element is the artifact.
                    .dropLast(1)
                    .joinToString(".")
            return "androidx.$group:${sections.last()}:$SNAPSHOT_MARKER"
        }

        throw GradleException("projectOrArtifact cannot find/replace project $path")
    }
}

const val SNAPSHOT_MARKER = "REPLACE_WITH_SNAPSHOT"
