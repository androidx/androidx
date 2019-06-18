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

package androidx.benchmark.gradle

import com.android.build.gradle.AppExtension
import com.android.build.gradle.BaseExtension
import com.android.build.gradle.LibraryExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.tasks.StopExecutionException

class BenchmarkPlugin : Plugin<Project> {
    private var foundAndroidPlugin = false

    override fun apply(project: Project) {
        // NOTE: Although none of the configuration code depends on a reference to the Android
        // plugin here, there is some implicit coupling behind the scenes, which ensures that the
        // required BaseExtension from AGP can be found by registering project configuration as a
        // PluginManager callback.

        project.pluginManager.withPlugin("com.android.application") {
            configureWithAndroidPlugin(project)
        }

        project.pluginManager.withPlugin("com.android.library") {
            configureWithAndroidPlugin(project)
        }

        // Verify that the configuration from this plugin dependent on AGP was successfully applied.
        project.afterEvaluate {
            if (!foundAndroidPlugin) {
                throw StopExecutionException(
                    """A required plugin, com.android.application or com.android.library was not
                        found. The androidx.benchmark plugin currently only supports android
                        application or library modules. Ensure that a required plugin is applied
                        in the project build.gradle file."""
                        .trimIndent()
                )
            }
        }
    }

    private fun configureWithAndroidPlugin(project: Project) {
        if (!foundAndroidPlugin) {
            foundAndroidPlugin = true
            val extension = project.extensions.getByType(BaseExtension::class.java)
            configureWithAndroidExtension(project, extension)
        }
    }

    private fun configureWithAndroidExtension(project: Project, extension: BaseExtension) {
        // Registering this block as a configureEach callback is only necessary because Studio skips
        // Gradle if there are no changes, which stops this plugin from being re-applied.
        var enabledOutput = false
        project.tasks.configureEach {
            if (!enabledOutput &&
                !project.rootProject.hasProperty("android.injected.invoked.from.ide")
            ) {
                enabledOutput = true

                // NOTE: This argument is checked by ResultWriter to enable CI reports.
                extension.defaultConfig.testInstrumentationRunnerArgument(
                    "androidx.benchmark.output.enable",
                    "true"
                )

                extension.defaultConfig.testInstrumentationRunnerArgument(
                    "no-isolated-storage",
                    "1"
                )
            }
        }

        project.tasks.register("lockClocks", LockClocksTask::class.java).configure {
            it.adbPath.set(extension.adbExecutable.absolutePath)
        }
        project.tasks.register("unlockClocks", UnlockClocksTask::class.java).configure {
            it.adbPath.set(extension.adbExecutable.absolutePath)
        }

        val extensionVariants = when (extension) {
            is AppExtension -> extension.applicationVariants
            is LibraryExtension -> extension.libraryVariants
            else -> throw StopExecutionException(
                """Missing required Android extension in project ${project.name}, this typically
                    means you are missing the required com.android.application or
                    com.android.library plugins or they could not be found. The
                    androidx.benchmark plugin currently only supports android application or
                    library modules. Ensure that the required plugin is applied in the project
                    build.gradle file.""".trimIndent()
            )
        }

        // NOTE: .all here is a Gradle API, which will run the callback passed to it after the
        // extension variants have been resolved.
        var applied = false
        extensionVariants.all {
            if (!applied) {
                applied = true

                project.tasks.register("benchmarkReport", BenchmarkReportTask::class.java)
                    .configure {
                        it.adbPath.set(extension.adbExecutable.absolutePath)
                        it.dependsOn(project.tasks.named("connectedAndroidTest"))
                    }

                project.tasks.named("connectedAndroidTest").configure {
                    configureWithConnectedAndroidTest(project, it)
                }
            }
        }
    }

    private fun configureWithConnectedAndroidTest(project: Project, connectedAndroidTest: Task) {
        // The task benchmarkReport must be registered by this point, and is responsible for
        // pulling report data from all connected devices onto host machine through adb.
        connectedAndroidTest.finalizedBy("benchmarkReport")

        var hasJetpackBenchmark = false

        project.configurations.matching { it.name.contains("androidTest") }.all {
            it.allDependencies.all { dependency ->
                if (dependency.name == "benchmark" && dependency.group == "androidx.benchmark") {
                    hasJetpackBenchmark = true
                }
            }
        }

        if (!hasJetpackBenchmark) {
            throw StopExecutionException(
                """Project ${project.name} missing required project dependency,
                    androidx.benchmark:benchmark. The androidx.benchmark plugin is meant to be
                    used in conjunction with the androix.benchmark library, but it was not found
                    within this project's dependencies. You can add the androidx.benchmark library
                    to your project by including androidTestImplementation
                    'androidx.benchmark:benchmark:<version>' in the dependencies block of the
                    project build.gradle file"""
                    .trimIndent()
            )
        }
    }
}
