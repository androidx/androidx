/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.inspection.gradle

import com.android.build.gradle.LibraryExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.StopExecutionException

/**
 * A plugin which, when present, ensures that intermediate inspector
 * resources are generated at build time
 */
class InspectionPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        var foundLibraryPlugin = false
        project.pluginManager.withPlugin("com.android.library") {
            foundLibraryPlugin = true
            val libExtension = project.extensions.getByType(LibraryExtension::class.java)
            includeMetaInfServices(libExtension)
        }
        project.afterEvaluate {
            if (!foundLibraryPlugin) {
                throw StopExecutionException(
                    """A required plugin, com.android.library, was not found.
                        The androidx.inspection plugin currently only supports android library
                        modules, so ensure that com.android.library is applied in the project
                        build.gradle file."""
                        .trimIndent()
                )
            }
        }
    }
}

private fun includeMetaInfServices(library: LibraryExtension) {
    library.sourceSets.getByName("main").resources.include("META-INF/services/*")
}