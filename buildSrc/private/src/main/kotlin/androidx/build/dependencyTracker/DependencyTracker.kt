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

package androidx.build.dependencyTracker

import java.io.Serializable
import org.gradle.api.logging.Logger

/**
 * Utility class that traverses all project dependencies and discover which modules depend on each
 * other. This is mainly used by [AffectedModuleDetector] to find out which projects should be run.
 *
 * @param dependentList A map from a project to the list of projects that depend on it. e.g. if
 *   project A depends on B, it is stored as B -> {A}.
 */
class DependencyTracker(private val dependentList: Map<String, Set<String>>, logger: Logger?) :
    Serializable {
    init {
        val stringBuilder = StringBuilder()
        dependentList.forEach { (project, dependents) ->
            dependents.forEach { dependent ->
                stringBuilder.append("there is a dependency from $dependent to $project\n")
            }
        }
        logger?.info(stringBuilder.toString())
    }

    fun findAllDependents(projectPath: String): Set<String> {
        val result = mutableSetOf<String>()
        fun addAllDependents(projectPath: String) {
            if (result.add(projectPath)) {
                dependentList[projectPath]?.forEach(::addAllDependents)
            }
        }
        addAllDependents(projectPath)
        // the projectPath isn't a dependent of itself
        return result.minus(projectPath)
    }
}
