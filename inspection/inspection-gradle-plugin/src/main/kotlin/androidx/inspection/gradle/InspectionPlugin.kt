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
import com.google.protobuf.gradle.GenerateProtoTask
import com.google.protobuf.gradle.ProtobufConvention
import com.google.protobuf.gradle.ProtobufPlugin
import com.google.protobuf.gradle.generateProtoTasks
import com.google.protobuf.gradle.protoc
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.tasks.StopExecutionException
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.getPlugin

/**
 * A plugin which, when present, ensures that intermediate inspector
 * resources are generated at build time
 */
class InspectionPlugin : Plugin<Project> {
    @ExperimentalStdlibApi
    override fun apply(project: Project) {
        var foundLibraryPlugin = false
        var foundReleaseVariant = false
        project.pluginManager.withPlugin("com.android.library") {
            foundLibraryPlugin = true
            val libExtension = project.extensions.getByType(LibraryExtension::class.java)
            includeMetaInfServices(libExtension)
            libExtension.libraryVariants.all { variant ->
                if (variant.name == "release") {
                    foundReleaseVariant = true
                    val unzip = project.registerUnzipTask(variant)
                    project.registerDexInspectorTask(variant, libExtension, unzip)
                }
            }
        }

        project.apply(plugin = "com.google.protobuf")
        project.plugins.all {
            if (it is ProtobufPlugin) {
                val protobufConvention = project.convention.getPlugin<ProtobufConvention>()
                protobufConvention.protobuf.apply {
                    protoc {
                        this.artifact = "com.google.protobuf:protoc:3.10.0"
                    }
                    generateProtoTasks {
                        all().forEach { task: GenerateProtoTask ->
                            task.builtins.create("java") { options ->
                                options.option("lite")
                            }
                        }
                    }
                }
            }
        }

        project.dependencies {
            add("implementation", "com.google.protobuf:protobuf-javalite:3.10.0")
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
            if (!foundReleaseVariant) {
                throw StopExecutionException("The androidx.inspection plugin requires " +
                        "release build variant.")
            }
        }
    }
}

private fun includeMetaInfServices(library: LibraryExtension) {
    library.sourceSets.getByName("main").resources.include("META-INF/services/*")
}