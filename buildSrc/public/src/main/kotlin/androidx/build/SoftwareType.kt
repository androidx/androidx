/*
 * Copyright 2020 The Android Open Source Project
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

import androidx.build.SoftwareType.Companion.BENCHMARK
import androidx.build.SoftwareType.Companion.SAMPLES
import androidx.build.SoftwareType.Companion.TEST_APPLICATION
import androidx.build.SoftwareType.Companion.UNSET
import kotlin.collections.contains

/**
 * Represents the purpose and configuration of a software project, including how it is published,
 * whether it enforces API compatibility checks, and which environment it targets. By using
 * [SoftwareType], developers can select from predefined library configurations or create their own
 * through [ConfigurableSoftwareType]. This reduces complexity by capturing a library's behavior and
 * rationale in one place, rather than requiring manual configuration of multiple independent
 * properties.
 *
 * The key properties controlled by [SoftwareType] are:
 * - [publish]: Defines how (or if) the software is published to external repositories (e.g.,
 *   GMaven).
 * - [checkApi]: Determines whether API compatibility tasks are run, which enforce semantic
 *   versioning and API stability.
 * - [compilationTarget]: Specifies whether the software runs on a host machine or an Android
 *   device.
 * - [allowCallingVisibleForTestsApis]: Indicates whether calling `@VisibleForTesting` APIs is
 *   allowed, useful for test libraries.
 * - [targetsKotlinConsumersOnly]: When `true`, the software is intended for Kotlin consumers only,
 *   allowing for more Kotlin-centric API design.
 * - [isForTesting]: When `true`, the library is intended to serve as a testing artifact only, not
 *   meant for usage in production.
 *
 * [SoftwareType] includes a variety of predefined configurations commonly used in Android projects:
 * - Conventional published libraries ([PUBLISHED_LIBRARY], [PUBLISHED_PROTO_LIBRARY], etc.)
 * - Internal libraries not published externally ([INTERNAL_TEST_LIBRARY],
 *   [INTERNAL_HOST_TEST_LIBRARY])
 * - Test libraries that allow testing internal or unstable APIs ([PUBLISHED_TEST_LIBRARY],
 *   [INTERNAL_TEST_LIBRARY])
 * - Lint rule sets ([LINT], [STANDALONE_PUBLISHED_LINT]) for guiding correct usage of a library
 * - Libraries containing samples to supplement documentation ([SAMPLES])
 * - Host-only libraries such as Gradle plugins, annotation processors, and code generators
 *   ([GRADLE_PLUGIN], [ANNOTATION_PROCESSOR], [OTHER_CODE_PROCESSOR])
 * - Libraries specifically meant for IDE consumption ([IDE_PLUGIN])
 * - Snapshot-only libraries for early access or development use cases
 *   ([SNAPSHOT_ONLY_LIBRARY_ONLY_USED_BY_KOTLIN_CONSUMERS], etc.)
 * - Libraries that do not publish artifacts but still run API tasks, or vice versa
 *   ([INTERNAL_LIBRARY_WITH_API_TASKS], [SNAPSHOT_ONLY_LIBRARY_WITH_API_TASKS])
 * - [UNSET]: a default or transitional state indicating the library's type isn't fully determined
 *
 * Although predefined software types cover many common scenarios, you can create new
 * [ConfigurableSoftwareType] instances if your project requires a unique combination of publish
 * settings, API checking, and compilation targeting. In doing so, you ensure the project's
 * configuration is concise, clear, and consistently applied.
 */
sealed class SoftwareType(
    val name: String,
    val publish: Publish = Publish.NONE,
    val checkApi: RunApiTasks = RunApiTasks.No("Unknown Software Type"),
    val compilationTarget: CompilationTarget = CompilationTarget.DEVICE,
    val allowCallingVisibleForTestsApis: Boolean = false,
    val targetsKotlinConsumersOnly: Boolean = false,
    val isForTesting: Boolean = false,
) {
    class ConfigurableSoftwareType(
        name: String,
        publish: Publish = Publish.NONE,
        checkApi: RunApiTasks = RunApiTasks.No("Unknown Software Type"),
        compilationTarget: CompilationTarget = CompilationTarget.DEVICE,
        allowCallingVisibleForTestsApis: Boolean = false,
        targetsKotlinConsumersOnly: Boolean = false,
        isForTesting: Boolean = true,
    ) :
        SoftwareType(
            name,
            publish,
            checkApi,
            compilationTarget,
            allowCallingVisibleForTestsApis,
            targetsKotlinConsumersOnly,
            isForTesting,
        )

    companion object {
        // Host-only tooling libraries
        @JvmStatic
        val ANNOTATION_PROCESSOR =
            ConfigurableSoftwareType(
                name = "ANNOTATION_PROCESSOR",
                publish = Publish.SNAPSHOT_AND_RELEASE,
                checkApi = RunApiTasks.No("Annotation Processor"),
                compilationTarget = CompilationTarget.HOST,
            )

        @JvmStatic
        val ANNOTATION_PROCESSOR_UTILS =
            ConfigurableSoftwareType(
                name = "ANNOTATION_PROCESSOR_UTILS",
                publish = Publish.SNAPSHOT_AND_RELEASE,
                checkApi = RunApiTasks.No("Annotation Processor Helper Library"),
                compilationTarget = CompilationTarget.HOST,
            )

        @JvmStatic
        val GRADLE_PLUGIN =
            ConfigurableSoftwareType(
                name = "GRADLE_PLUGIN",
                publish = Publish.SNAPSHOT_AND_RELEASE,
                checkApi = RunApiTasks.No("Gradle Plugin (Host-only)"),
                compilationTarget = CompilationTarget.HOST,
            )

        @JvmStatic
        val OTHER_CODE_PROCESSOR =
            ConfigurableSoftwareType(
                name = "OTHER_CODE_PROCESSOR",
                publish = Publish.SNAPSHOT_AND_RELEASE,
                checkApi = RunApiTasks.No("Code Processor (Host-only)"),
                compilationTarget = CompilationTarget.HOST,
            )

        // Lint libraries
        @JvmStatic
        val LINT =
            ConfigurableSoftwareType(
                name = "LINT",
                checkApi = RunApiTasks.No("Lint Library"),
                compilationTarget = CompilationTarget.HOST,
            )

        @JvmStatic
        val STANDALONE_PUBLISHED_LINT =
            ConfigurableSoftwareType(
                name = "STANDALONE_PUBLISHED_LINT",
                publish = Publish.SNAPSHOT_AND_RELEASE,
                checkApi = RunApiTasks.No("Lint Library"),
                compilationTarget = CompilationTarget.HOST,
            )

        // Published libraries
        @JvmStatic
        val PUBLISHED_LIBRARY =
            ConfigurableSoftwareType(
                name = "PUBLISHED_LIBRARY",
                publish = Publish.SNAPSHOT_AND_RELEASE,
                checkApi = RunApiTasks.Yes(),
            )

        @JvmStatic
        val PUBLISHED_PROTO_LIBRARY =
            ConfigurableSoftwareType(
                name = "PUBLISHED_PROTO_LIBRARY",
                publish = Publish.SNAPSHOT_AND_RELEASE,
                checkApi =
                    RunApiTasks.No("Metalava doesn't properly parse the proto sources b/180579063"),
            )

        @JvmStatic
        val PUBLISHED_LIBRARY_ONLY_USED_BY_KOTLIN_CONSUMERS =
            ConfigurableSoftwareType(
                name = "PUBLISHED_LIBRARY_ONLY_USED_BY_KOTLIN_CONSUMERS",
                publish = Publish.SNAPSHOT_AND_RELEASE,
                checkApi = RunApiTasks.Yes(),
                targetsKotlinConsumersOnly = true,
            )

        // Published test libraries
        @JvmStatic
        val PUBLISHED_TEST_LIBRARY =
            ConfigurableSoftwareType(
                name = "PUBLISHED_TEST_LIBRARY",
                publish = Publish.SNAPSHOT_AND_RELEASE,
                checkApi = RunApiTasks.Yes(),
                allowCallingVisibleForTestsApis = true,
                isForTesting = true,
            )

        @JvmStatic
        val PUBLISHED_KOTLIN_ONLY_TEST_LIBRARY =
            ConfigurableSoftwareType(
                name = "PUBLISHED_KOTLIN_ONLY_TEST_LIBRARY",
                publish = Publish.SNAPSHOT_AND_RELEASE,
                checkApi = RunApiTasks.Yes(),
                allowCallingVisibleForTestsApis = true,
                targetsKotlinConsumersOnly = true,
                isForTesting = true,
            )

        // Snapshot-only libraries
        @JvmStatic
        val SNAPSHOT_ONLY_LIBRARY_ONLY_USED_BY_KOTLIN_CONSUMERS =
            ConfigurableSoftwareType(
                name = "SNAPSHOT_ONLY_LIBRARY_ONLY_USED_BY_KOTLIN_CONSUMERS",
                publish = Publish.SNAPSHOT_ONLY,
                checkApi = RunApiTasks.No("Snapshot-only library that does not run API tasks"),
                targetsKotlinConsumersOnly = true,
            )

        @JvmStatic
        val SNAPSHOT_ONLY_TEST_LIBRARY_WITH_API_TASKS =
            ConfigurableSoftwareType(
                name = "SNAPSHOT_ONLY_TEST_LIBRARY_WITH_API_TASKS",
                publish = Publish.SNAPSHOT_ONLY,
                checkApi = RunApiTasks.Yes(),
                allowCallingVisibleForTestsApis = true,
            )

        @JvmStatic
        val SNAPSHOT_ONLY_LIBRARY_WITH_API_TASKS =
            ConfigurableSoftwareType(
                name = "SNAPSHOT_ONLY_LIBRARY_WITH_API_TASKS",
                publish = Publish.SNAPSHOT_ONLY,
                checkApi = RunApiTasks.Yes("Snapshot-only library that runs API tasks"),
            )

        @JvmStatic
        val SNAPSHOT_ONLY_LIBRARY =
            ConfigurableSoftwareType(
                name = "SNAPSHOT_ONLY_LIBRARY",
                publish = Publish.SNAPSHOT_ONLY,
                checkApi = RunApiTasks.No("Snapshot-only library that does not run API tasks"),
            )

        // Samples library
        @JvmStatic
        val SAMPLES =
            ConfigurableSoftwareType(
                name = "SAMPLES",
                publish = Publish.SNAPSHOT_AND_RELEASE,
                checkApi = RunApiTasks.No("Sample Library"),
            )

        // IDE libraries
        @JvmStatic
        val IDE_PLUGIN =
            ConfigurableSoftwareType(
                name = "IDE_PLUGIN",
                checkApi = RunApiTasks.No("IDE Plugin (consumed only by Android Studio)"),
                compilationTarget = CompilationTarget.DEVICE,
            )

        // Internal libraries
        @JvmStatic
        val INTERNAL_GRADLE_PLUGIN =
            ConfigurableSoftwareType(
                name = "INTERNAL_GRADLE_PLUGIN",
                checkApi = RunApiTasks.No("Internal Gradle Plugin"),
                compilationTarget = CompilationTarget.HOST,
            )

        @JvmStatic
        val INTERNAL_HOST_TEST_LIBRARY =
            ConfigurableSoftwareType(
                name = "INTERNAL_HOST_TEST_LIBRARY",
                checkApi = RunApiTasks.No("Internal Library"),
                compilationTarget = CompilationTarget.HOST,
                isForTesting = true,
            )

        @JvmStatic
        val INTERNAL_LIBRARY_WITH_API_TASKS =
            ConfigurableSoftwareType(
                name = "INTERNAL_LIBRARY_WITH_API_TASKS",
                checkApi = RunApiTasks.Yes("Always run API tasks even if not published"),
            )

        @JvmStatic
        val INTERNAL_OTHER_CODE_PROCESSOR =
            ConfigurableSoftwareType(
                name = "INTERNAL_OTHER_CODE_PROCESSOR",
                checkApi = RunApiTasks.No("Code Processor (Host-only)"),
                compilationTarget = CompilationTarget.HOST,
            )

        @JvmStatic
        val INTERNAL_TEST_LIBRARY =
            ConfigurableSoftwareType(
                name = "INTERNAL_TEST_LIBRARY",
                checkApi = RunApiTasks.No("Internal Library"),
                allowCallingVisibleForTestsApis = true,
                isForTesting = true,
            )

        // Misc libraries
        @JvmStatic
        val BENCHMARK =
            ConfigurableSoftwareType(
                name = "BENCHMARK",
                checkApi = RunApiTasks.No("Benchmark Library"),
                allowCallingVisibleForTestsApis = true,
            )

        @JvmStatic
        val TEST_APPLICATION =
            ConfigurableSoftwareType(
                name = "TEST_APPLICATION",
                checkApi = RunApiTasks.No("Test App"),
            )

        val UNSET = ConfigurableSoftwareType(name = "UNSET")

        private val allTypes: Map<String, SoftwareType> by lazy {
            listOf(
                    PUBLISHED_LIBRARY,
                    PUBLISHED_PROTO_LIBRARY,
                    PUBLISHED_LIBRARY_ONLY_USED_BY_KOTLIN_CONSUMERS,
                    PUBLISHED_TEST_LIBRARY,
                    PUBLISHED_KOTLIN_ONLY_TEST_LIBRARY,
                    INTERNAL_GRADLE_PLUGIN,
                    INTERNAL_HOST_TEST_LIBRARY,
                    INTERNAL_LIBRARY_WITH_API_TASKS,
                    INTERNAL_OTHER_CODE_PROCESSOR,
                    INTERNAL_TEST_LIBRARY,
                    SAMPLES,
                    SNAPSHOT_ONLY_LIBRARY,
                    SNAPSHOT_ONLY_LIBRARY_WITH_API_TASKS,
                    SNAPSHOT_ONLY_LIBRARY_ONLY_USED_BY_KOTLIN_CONSUMERS,
                    SNAPSHOT_ONLY_TEST_LIBRARY_WITH_API_TASKS,
                    TEST_APPLICATION,
                    LINT,
                    STANDALONE_PUBLISHED_LINT,
                    GRADLE_PLUGIN,
                    ANNOTATION_PROCESSOR,
                    ANNOTATION_PROCESSOR_UTILS,
                    BENCHMARK,
                    OTHER_CODE_PROCESSOR,
                    IDE_PLUGIN,
                    UNSET,
                )
                .associateBy { it.name }
        }

        fun valueOf(name: String): SoftwareType {
            return requireNotNull(allTypes[name]) { "SoftwareType with name $name not found" }
        }
    }
}

fun SoftwareType.requiresDependencyVerification(): Boolean =
    this !in listOf(BENCHMARK, SAMPLES, TEST_APPLICATION, UNSET)

enum class CompilationTarget {
    /** This library is meant to run on the host machine (like an annotation processor). */
    HOST,
    /** This library is meant to run on an Android device. */
    DEVICE,
}

/**
 * Publish Enum: Publish.NONE -> Generates no artifacts; does not generate snapshot artifacts or
 * releasable maven artifacts Publish.SNAPSHOT_ONLY -> Only generates snapshot artifacts
 * Publish.SNAPSHOT_AND_RELEASE -> Generates both snapshot artifacts and releasable maven artifact
 */
enum class Publish {
    NONE,
    SNAPSHOT_ONLY,
    SNAPSHOT_AND_RELEASE;

    fun shouldRelease() = this == SNAPSHOT_AND_RELEASE

    fun shouldPublish() = shouldRelease() || this == SNAPSHOT_ONLY
}

sealed class RunApiTasks {

    /** Always run API tasks regardless of other project properties. */
    data class Yes(val reason: String? = null) : RunApiTasks()

    /** Do not run any API tasks. */
    data class No(val reason: String) : RunApiTasks()
}

fun SoftwareType.isLint() =
    this == SoftwareType.LINT || this == SoftwareType.STANDALONE_PUBLISHED_LINT
