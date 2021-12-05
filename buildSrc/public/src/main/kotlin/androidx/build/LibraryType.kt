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

/**
 * LibraryType represents the purpose and type of a library, whether it is a conventional library,
 * a set of samples showing how to use a conventional library, a set of lint rules for using a
 * conventional library, or any other type of published project.
 *
 * LibraryType collects a set of properties together, to make the "why" more clear and to simplify
 * setting these properties for library developers, so that only a single enum inferrable from
 * the purpose of the library needs to be set, rather than a variety of more arcane options.
 *
 * These properties are as follows:
 * LibraryType.publish represents how the library is published to GMaven
 * LibraryType.sourceJars represents whether we publish the source code for the library to GMaven
 *      in a way accessible to download, such as by Android Studio
 * LibraryType.generateDocs represents whether we generate documentation from the library to put on
 *      developer.android.com
 * LibraryType.checkApi represents whether we enforce API compatibility of the library according
 *      to our semantic versioning protocol
 *
 * The possible values of LibraryType are as follows:
 * PUBLISHED_LIBRARY: a conventional library, published, sourced, documented, and versioned.
 * SAMPLES: a library of samples, published as additional properties to a conventional library,
 *      including published source. Documented in a special way, not API tracked.
 * LINT: a library of lint rules for using a conventional library. Published through lintPublish as
 *      part of an AAR, not published standalone.
 * COMPILER_PLUGIN: a tool that modifies the kotlin or java compiler. Used only while compiling.
 * GRADLE_PLUGIN: a library that is a gradle plugin.
 * ANNOTATION_PROCESSOR: a library consisting of an annotation processor. Used only while compiling.
 * OTHER_CODE_PROCESSOR: a library that algorithmically generates and/or alters code
 *                      but not through hooking into custom annotations or the kotlin compiler.
 *                      For example, navigation:safe-args-generator or Jetifier.
 * UNSET: a library that has not yet been migrated to using LibraryType. Should never be used.
 *
 * TODO: potential future LibraryTypes:
 * KOTLIN_ONLY_LIBRARY: like PUBLISHED_LIBRARY, but not intended for use from java. ktx and compose.
 * INTERNAL_TEST
 * DEMO
 * IDE_PLUGIN
 *
 */
enum class LibraryType(
    val publish: Publish = Publish.NONE,
    val sourceJars: Boolean = false,
    val checkApi: RunApiTasks = RunApiTasks.No("Unknown Library Type"),
    val compilationTarget: CompilationTarget = CompilationTarget.DEVICE
) {
    PUBLISHED_LIBRARY(
        publish = Publish.SNAPSHOT_AND_RELEASE,
        sourceJars = true,
        checkApi = RunApiTasks.Yes()
    ),
    PUBLISHED_TEST_LIBRARY(
        publish = Publish.SNAPSHOT_AND_RELEASE,
        sourceJars = true,
        checkApi = RunApiTasks.Yes()
    ),
    INTERNAL_TEST_LIBRARY(
        checkApi = RunApiTasks.No("Internal Library")
    ),
    SAMPLES(
        publish = Publish.SNAPSHOT_AND_RELEASE,
        sourceJars = true,
        checkApi = RunApiTasks.No("Sample Library")
    ),
    LINT(
        publish = Publish.NONE,
        sourceJars = false,
        checkApi = RunApiTasks.No("Lint Library"),
        compilationTarget = CompilationTarget.HOST
    ),
    COMPILER_PLUGIN(
        Publish.SNAPSHOT_AND_RELEASE,
        sourceJars = false,
        RunApiTasks.No("Compiler Plugin (Host-only)"),
        CompilationTarget.HOST
    ),
    GRADLE_PLUGIN(
        Publish.SNAPSHOT_AND_RELEASE,
        sourceJars = false,
        RunApiTasks.No("Gradle Plugin (Host-only)"),
        CompilationTarget.HOST
    ),
    ANNOTATION_PROCESSOR(
        publish = Publish.SNAPSHOT_AND_RELEASE,
        sourceJars = false,
        checkApi = RunApiTasks.No("Annotation Processor"),
        compilationTarget = CompilationTarget.HOST
    ),
    ANNOTATION_PROCESSOR_UTILS(
        publish = Publish.SNAPSHOT_AND_RELEASE,
        sourceJars = true,
        checkApi = RunApiTasks.No("Annotation Processor Helper Library"),
        compilationTarget = CompilationTarget.HOST
    ),
    OTHER_CODE_PROCESSOR(
        publish = Publish.SNAPSHOT_AND_RELEASE,
        sourceJars = false,
        checkApi = RunApiTasks.No("Code Processor (Host-only)"),
        compilationTarget = CompilationTarget.HOST
    ),
    IDE_PLUGIN(
        publish = Publish.NONE,
        sourceJars = false,
        // TODO: figure out a way to make sure we don't break Studio
        checkApi = RunApiTasks.No("IDE Plugin (consumed only by Android Studio"),
        // This is a bit complicated. IDE plugins usually have an on-device component installed by
        // Android Studio, rather than by a client of the library, but also a host-side component.
        compilationTarget = CompilationTarget.DEVICE
    ),
    UNSET()
}

enum class CompilationTarget {
    /** This library is meant to run on the host machine (like an annotation processor). */
    HOST,
    /** This library is meant to run on an Android device. */
    DEVICE
}

/**
 * Publish Enum:
 * Publish.NONE -> Generates no aritfacts; does not generate snapshot artifacts
 *                 or releasable maven artifacts
 * Publish.SNAPSHOT_ONLY -> Only generates snapshot artifacts
 * Publish.SNAPSHOT_AND_RELEASE -> Generates both snapshot artifacts and releasable maven artifact
 * Publish.UNSET -> Do the default, based on LibraryType. If LibraryType.UNSET -> Publish.NONE
 *
 * TODO: should we introduce a Publish.lintPublish?
 * TODO: remove Publish.UNSET once we remove LibraryType.UNSET.
 * It is necessary now in order to be able to override LibraryType.publish (with Publish.None)
 */
enum class Publish {
    NONE, SNAPSHOT_ONLY, SNAPSHOT_AND_RELEASE, UNSET;

    fun shouldRelease() = this == SNAPSHOT_AND_RELEASE
    fun shouldPublish() = this == SNAPSHOT_ONLY || this == SNAPSHOT_AND_RELEASE
}

sealed class RunApiTasks {
    /** Automatically determine whether API tasks should be run. */
    object Auto : RunApiTasks()
    /** Always run API tasks regardless of other project properties. */
    data class Yes(val reason: String? = null) : RunApiTasks()
    /** Do not run any API tasks. */
    data class No(val reason: String) : RunApiTasks()
}
