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

import java.io.File
import java.util.concurrent.atomic.AtomicBoolean
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.file.Directory
import org.gradle.api.provider.Provider
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinMultiplatformPluginWrapper
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget

/**
 * The Darwin benchmark plugin that helps run KMP benchmarks on iOS devices, and extracts benchmark
 * results in `json`.
 */
class DarwinBenchmarkPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        val extension =
            project.extensions.create("darwinBenchmark", DarwinBenchmarkPluginExtension::class.java)

        val appliedDarwinPlugin = AtomicBoolean()

        project.plugins.withType(KotlinMultiplatformPluginWrapper::class.java) {
            val multiplatformExtension: KotlinMultiplatformExtension =
                project.extensions.getByType(it.projectExtensionClass.java)
            multiplatformExtension.targets.all { kotlinTarget ->
                if (kotlinTarget is KotlinNativeTarget) {
                    if (kotlinTarget.konanTarget.family.isAppleFamily) {
                        // We want to apply the plugin only once.
                        if (appliedDarwinPlugin.compareAndSet(false, true)) {
                            applyDarwinPlugin(extension, project)
                        }
                    }
                }
            }
        }
    }

    private fun applyDarwinPlugin(
        extension: DarwinBenchmarkPluginExtension,
        project: Project
    ) {
        // You can override the xcodeGenDownloadUri by specifying something like:
        // androidx.benchmark.darwin.xcodeGenDownloadUri=https://github.com/yonaskolb/XcodeGen/releases/download/2.32.0/xcodegen.zip
        val xcodeGenUri = when (val uri = project.findProperty(XCODEGEN_DOWNLOAD_URI)) {
            null -> File(
                project.rootProject.projectDir, // frameworks/support
                "../../prebuilts/androidx/external/xcodegen"
            ).absoluteFile.toURI().toString()

            else -> uri.toString()
        }

        val xcodeProjectPath = extension.xcodeProjectName.flatMap { name ->
            project.layout.buildDirectory.dir("$name.xcodeproj")
        }

        val xcResultPath = extension.xcodeProjectName.flatMap { name ->
            project.layout.buildDirectory.dir("$name.xcresult")
        }

        // Configure the XCode Build Service so we don't run too many benchmarks at the same time.
        project.configureXCodeBuildService()

        val fetchXCodeGenTask = project.tasks.register(
            FETCH_XCODEGEN_TASK, FetchXCodeGenTask::class.java
        ) {
            it.xcodeGenUri.set(xcodeGenUri)
            it.downloadPath.set(project.layout.buildDirectory.dir("xcodegen"))
        }

        val generateXCodeProjectTask = project.tasks.register(
            GENERATE_XCODE_PROJECT_TASK, GenerateXCodeProjectTask::class.java
        ) {
            it.xcodeGenPath.set(fetchXCodeGenTask.map { task -> task.xcodeGenBinary() })
            it.yamlFile.set(extension.xcodeGenConfigFile)
            it.projectName.set(extension.xcodeProjectName)
            it.xcProjectPath.set(xcodeProjectPath)
        }

        val runDarwinBenchmarks = project.tasks.register(
            RUN_DARWIN_BENCHMARKS_TASK, RunDarwinBenchmarksTask::class.java
        ) {
            val sharedService =
                project
                    .gradle
                    .sharedServices
                    .registrations.getByName(XCodeBuildService.XCODE_BUILD_SERVICE_NAME)
                    .service
            it.usesService(sharedService)
            it.xcodeProjectPath.set(generateXCodeProjectTask.flatMap { task ->
                task.xcProjectPath
            })
            it.destination.set(extension.destination)
            it.scheme.set(extension.scheme)
            it.xcResultPath.set(xcResultPath)
            it.dependsOn(XCFRAMEWORK_TASK_NAME)
        }

        project.tasks.register(
            DARWIN_BENCHMARK_RESULTS_TASK, DarwinBenchmarkResultsTask::class.java
        ) {
            it.group = "Verification"
            it.description = "Run Kotlin Multiplatform Benchmarks for Darwin"
            it.xcResultPath.set(runDarwinBenchmarks.flatMap { task ->
                task.xcResultPath
            })
            it.referenceSha.set(extension.referenceSha)
            val resultFileName = "${extension.xcodeProjectName.get()}-benchmark-result.json"
            it.outputFile.set(
                project.layout.buildDirectory.file(
                    resultFileName
                )
            )
            val provider = project.benchmarksDistributionDirectory(extension)
            val resultFile = provider.map { directory ->
                directory.file(resultFileName)
            }
            it.distFile.set(resultFile)
        }
    }

    private fun Project.benchmarksDistributionDirectory(
        extension: DarwinBenchmarkPluginExtension
    ): Provider<Directory> {
        val distProvider = project.distributionDirectory()
        val benchmarksDirProvider = distProvider.flatMap { distDir ->
            extension.xcodeProjectName.map { projectName ->
                val projectPath = project.path.replace(":", "/")
                val benchmarksDirectory = File(distDir, DARWIN_BENCHMARKS_DIR)
                File(benchmarksDirectory, "$projectPath/$projectName")
            }
        }
        return project.layout.dir(benchmarksDirProvider)
    }

    private fun Project.distributionDirectory(): Provider<File> {
        // We want to write metrics to library metrics specific location
        // Context: b/257326666
        return providers.environmentVariable(DIST_DIR).map { value ->
            val parent = value.ifBlank {
                @Suppress("DEPRECATION") // b/290811136
                project.buildDir.absolutePath
            }
            File(parent, LIBRARY_METRICS)
        }
    }

    private companion object {
        // Environment variables
        const val DIST_DIR = "DIST_DIR"
        const val LIBRARY_METRICS = "librarymetrics"
        const val DARWIN_BENCHMARKS_DIR = "darwinBenchmarks"

        // Gradle Properties
        const val XCODEGEN_DOWNLOAD_URI = "androidx.benchmark.darwin.xcodeGenDownloadUri"

        // Tasks
        const val FETCH_XCODEGEN_TASK = "fetchXCodeGen"
        const val GENERATE_XCODE_PROJECT_TASK = "generateXCodeProject"
        const val RUN_DARWIN_BENCHMARKS_TASK = "runDarwinBenchmarks"
        const val DARWIN_BENCHMARK_RESULTS_TASK = "darwinBenchmarkResults"

        // There is always a XCFrameworkConfig with the name `AndroidXDarwinBenchmarks`
        // The corresponding task name is `assemble{name}ReleaseXCFramework`
        const val XCFRAMEWORK_TASK_NAME = "assembleAndroidXDarwinBenchmarksReleaseXCFramework"
    }
}
