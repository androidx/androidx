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

package org.jetbrains.androidx.build

import org.jetbrains.androidx.build.JetBrainsPublication.projectPathToLibrary
import java.io.Serializable
import org.gradle.api.Project

/**
 * Library groups and associated with them projects and targets that are published when
 * building the JetBrains fork of AOSP.
 */
object JetBrainsPublication {
    private const val ANDROIDX_GROUP_PREFIX = "androidx."
    private const val JETBRAINS_COMPOSE_GROUP_PREFIX = "org.jetbrains.compose."
    private const val JETBRAINS_FORK_GROUP_PREFIX = "org.jetbrains.androidx."

    val libraryToComponents = mapOf(
        "COMPOSE" to listOf(
            ComposeComponent(":compose:animation:animation"),
            ComposeComponent(":compose:animation:animation-core"),
            ComposeComponent(":compose:animation:animation-graphics"),
            ComposeComponent(":compose:foundation:foundation"),
            ComposeComponent(":compose:foundation:foundation-layout"),
            ComposeComponent(":compose:material:material"),
            ComposeComponent(":compose:material:material-navigation"),
            ComposeComponent(":compose:material:material-ripple"),
            ComposeComponent(":compose:runtime:runtime", supportedPlatforms = ComposePlatforms.ALL),
            ComposeComponent(":compose:runtime:runtime-saveable", supportedPlatforms = ComposePlatforms.ALL),
            ComposeComponent(":compose:ui:ui"),
            ComposeComponent(":compose:ui:ui-geometry"),
            ComposeComponent(
                path = ":compose:ui:ui-backhandler",
                supportedPlatforms = ComposePlatforms.SKIKO_SUPPORT,
            ),
            ComposeComponent(":compose:ui:ui-graphics"),
            ComposeComponent(":compose:ui:ui-test"),
            ComposeComponent(
                ":compose:ui:ui-test-junit4",
                supportedPlatforms = ComposePlatforms.JVM_BASED
            ),
            ComposeComponent(":compose:ui:ui-text"),
            ComposeComponent(":compose:ui:ui-tooling", supportedPlatforms = ComposePlatforms.JVM_BASED),
            ComposeComponent(
                ":compose:ui:ui-tooling-data",
                supportedPlatforms = ComposePlatforms.JVM_BASED
            ),
            ComposeComponent(":compose:ui:ui-tooling-preview"),
            ComposeComponent(
                ":compose:ui:ui-uikit",
                supportedPlatforms = ComposePlatforms.IOS
            ),
            ComposeComponent(":compose:ui:ui-unit"),
            ComposeComponent(":compose:ui:ui-util"),
            ComposeComponent(
                ":compose:desktop:desktop",
                supportedPlatforms = setOf(ComposePlatforms.Desktop),
                customTasks = listOf(
                    "KotlinMultiplatform",
                    "Jvm",
                    "Jvmlinux-x64",
                    "Jvmlinux-arm64",
                    "Jvmmacos-x64",
                    "Jvmmacos-arm64",
                    "Jvmwindows-x64",
                    "Jvmwindows-arm64",
                )
            ),
        ),
        "COMPOSE_MATERIAL3" to listOf(
            ComposeComponent(":compose:material3:material3"),
            ComposeComponent(":compose:material3:material3-window-size-class"),
            ComposeComponent(":compose:material3:material3-adaptive-navigation-suite"),
        ),
        "COMPOSE_MATERIAL3_ADAPTIVE" to listOf(
            ComposeComponent(":compose:material3:adaptive:adaptive"),
            ComposeComponent(":compose:material3:adaptive:adaptive-layout"),
            ComposeComponent(":compose:material3:adaptive:adaptive-navigation"),
            ComposeComponent(":compose:material3:adaptive:adaptive-navigation3"),
        ),
        "LIFECYCLE" to listOf(
            ComposeComponent(
                path = ":lifecycle:lifecycle-common",
                // No android target here - jvm artefact will be used for android apps as well
                supportedPlatforms = ComposePlatforms.ALL - ComposePlatforms.ANDROID
            ),
            ComposeComponent(
                path = ":lifecycle:lifecycle-runtime",
                supportedPlatforms = ComposePlatforms.ALL
            ),
            ComposeComponent(
                path = ":lifecycle:lifecycle-viewmodel",
                supportedPlatforms = ComposePlatforms.ALL
            ),
            ComposeComponent(":lifecycle:lifecycle-viewmodel-savedstate", supportedPlatforms = ComposePlatforms.ALL),
            ComposeComponent(":lifecycle:lifecycle-runtime-compose", supportedPlatforms = ComposePlatforms.ALL),
            ComposeComponent(":lifecycle:lifecycle-viewmodel-compose", supportedPlatforms = ComposePlatforms.ALL),
            ComposeComponent(":lifecycle:lifecycle-viewmodel-navigation3", supportedPlatforms = ComposePlatforms.ALL),
        ),
        "NAVIGATION" to listOf(
            ComposeComponent(":navigation:navigation-compose"),
            ComposeComponent(":navigation:navigation-common", supportedPlatforms = ComposePlatforms.ALL - ComposePlatforms.WINDOWS_NATIVE),
            ComposeComponent(":navigation:navigation-runtime", supportedPlatforms = ComposePlatforms.ALL - ComposePlatforms.WINDOWS_NATIVE),
        ),
        "NAVIGATION_3" to listOf(
            ComposeComponent(":navigation3:navigation3-ui"),
        ),
        "NAVIGATION_EVENT" to listOf(
            ComposeComponent(":navigationevent:navigationevent-compose", supportedPlatforms = ComposePlatforms.ALL),
        ),
        "SAVEDSTATE" to listOf(
            ComposeComponent(":savedstate:savedstate", supportedPlatforms = ComposePlatforms.ALL),
            ComposeComponent(":savedstate:savedstate-compose", supportedPlatforms = ComposePlatforms.ALL),
        ),
    )

    private val jetBrainsProjectsWithAndroidTarget = setOf(
        ":compose:ui:ui-backhandler",
    )

    init {
        val allPaths = libraryToComponents.flatMap { it.value }.map { it.path }
        val nonUniquePaths = allPaths - allPaths.distinct()
        require(nonUniquePaths.isEmpty()) {
            "All components paths should be unique. Non-unique paths: $nonUniquePaths"
        }
    }

    fun mavenGroupFor(projectPath: String): String = when {
        projectPath.startsWith(":compose:") ->
            JETBRAINS_COMPOSE_GROUP_PREFIX + projectPath
                .removePrefix(":compose:")
                .substringBeforeLast(":")
                .replace(":", ".")
        projectPath.startsWith(":") ->
            JETBRAINS_FORK_GROUP_PREFIX + projectPath
                .removePrefix(":")
                .substringBeforeLast(":")
                .replace(":", ".")
        else -> error("Unknown group replacement for projectPath=$projectPath")
    }

    fun projectPathForCoordinates(group: String, name: String): String? = when {
        isAndroidXGroup(group) ->
            ":${group.removePrefix(ANDROIDX_GROUP_PREFIX).replace(".", ":")}:$name"
        group.startsWith(JETBRAINS_COMPOSE_GROUP_PREFIX) ->
            ":compose:${group.removePrefix(JETBRAINS_COMPOSE_GROUP_PREFIX).replace(".", ":")}:$name"
        group.startsWith(JETBRAINS_FORK_GROUP_PREFIX) ->
            ":${group.removePrefix(JETBRAINS_FORK_GROUP_PREFIX).replace(".", ":")}:$name"
        else -> null
    }

    fun isAndroidXGroup(group: String): Boolean = group.startsWith(ANDROIDX_GROUP_PREFIX)

    fun isJetBrainsForkGroup(group: String): Boolean =
        group.startsWith(JETBRAINS_FORK_GROUP_PREFIX) || group.startsWith(JETBRAINS_COMPOSE_GROUP_PREFIX)

    val projectPathToComponent: Map<String, ComposeComponent> = libraryToComponents.values
        .flatten().associateBy { it.path }

    val projectPathToLibrary: Map<String, String> = libraryToComponents.entries
        .flatMap { entry -> entry.value.map { entry.key to it  } }
        .associate { it.second.path to it.first }

    fun shouldPublish(project: Project): Boolean = shouldPublish(project.path)
    fun shouldPublish(projectPath: String): Boolean = projectPathToComponent.containsKey(projectPath)

    fun isLibraryRegistered(libraryName: String) =
        libraryToComponents.containsKey(libraryName)

    fun isJetBrainsProjectWithAndroidTarget(project: Project) =
        jetBrainsProjectsWithAndroidTarget.contains(project.path)
}

/**
 * A set of version that can be assigned to publishing libraries from [JetBrainsPublication].
 * Only registered libraries are allowed
 * (use [JetBrainsPublication.isLibraryRegistered] to check)
 */
class JetBrainsVersions(val libraryToVersion: Map<String, String>) : Serializable {
    init {
        val nonRegisteredLibraries =
            libraryToVersion.keys.filterNot(JetBrainsPublication::isLibraryRegistered)
        require(nonRegisteredLibraries.isEmpty()) {
            "Libraries $nonRegisteredLibraries are not registered in the JetBrainsPublication class"
        }
    }

    fun versionOf(libraryName: String): String {
        return libraryToVersion[libraryName] ?: "9999.0.0-SNAPSHOT"
    }
}

fun ComposeComponent.library() = requireNotNull(projectPathToLibrary[path]) {
    "Library for component with path $path not found"
}
