/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.benchmark.darwin.gradle

import org.gradle.api.Plugin
import org.gradle.api.Project

/**
 * The Darwin benchmark plugin that helps run KMP benchmarks on iOS devices, and extracts benchmark
 * results in `json`.
 */
class DarwinBenchmarkPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        val extension =
            project.extensions.create("darwinBenchmark", DarwinBenchmarkPluginExtension::class.java)

        val xcodeProjectPath = extension.xcodeProjectName.flatMap { name ->
            project.layout.buildDirectory.dir("$name.xcodeproj")
        }

        val xcResultPath = extension.xcodeProjectName.flatMap { name ->
            project.layout.buildDirectory.dir("$name.xcresult")
        }

        val generateXCodeProjectTask = project.tasks.register(
            GENERATE_XCODE_PROJECT_TASK, GenerateXCodeProjectTask::class.java
        ) {
            it.yamlFile.set(extension.xcodeGenConfigFile)
            it.projectName.set(extension.xcodeProjectName)
            it.xcProjectPath.set(xcodeProjectPath)
            it.infoPlistPath.set(project.layout.buildDirectory.file("Info.plist"))
        }

        val runDarwinBenchmarks = project.tasks.register(
            RUN_DARWIN_BENCHMARKS_TASK, RunDarwinBenchmarksTask::class.java
        ) {
            it.xcodeProjectPath.set(generateXCodeProjectTask.flatMap { task ->
                task.xcProjectPath
            })
            it.destination.set(extension.destination)
            it.scheme.set(extension.scheme)
            it.xcResultPath.set(xcResultPath)
            it.dependsOn("assemble${extension.xcFrameworkConfig.get()}ReleaseXCFramework")
        }

        project.tasks.register(
            DARWIN_BENCHMARK_RESULTS_TASK, DarwinBenchmarkResultsTask::class.java
        ) {
            it.xcResultPath.set(runDarwinBenchmarks.flatMap { task ->
                task.xcResultPath
            })
            it.outputFile.set(
                project.layout.buildDirectory.file(
                    "${extension.xcodeProjectName.get()}-benchmark-result.json"
                )
            )
        }
    }

    private companion object {
        const val GENERATE_XCODE_PROJECT_TASK = "generateXCodeProject"
        const val RUN_DARWIN_BENCHMARKS_TASK = "runDarwinBenchmarks"
        const val DARWIN_BENCHMARK_RESULTS_TASK = "darwinBenchmarkResults"
    }
}
