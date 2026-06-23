/*
 * Copyright 2026 The Android Open Source Project
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

import androidx.build.AndroidXMultiplatformExtension
import androidx.build.ProjectLayoutType.Companion.isJetBrainsFork
import org.gradle.api.Project
import org.gradle.api.artifacts.CapabilityResolutionDetails
import org.gradle.api.artifacts.ComponentMetadataContext
import org.gradle.api.artifacts.ComponentMetadataRule
import org.gradle.api.artifacts.component.ModuleComponentIdentifier
import org.gradle.api.artifacts.component.ProjectComponentIdentifier

/**
 * Gradle component metadata rule that adds capabilities to resolve conflicts between
 * forked org.jetbrains.androidx.* artifacts and original androidx.* artifacts.
 *
 * This ensures that when both variants exist in the dependency graph, Gradle can
 * properly resolve the conflict, with project references taking precedence over
 * external dependencies.
 */
private class JetBrainsCapabilityRule : ComponentMetadataRule {
    override fun execute(context: ComponentMetadataContext): Unit = context.details.run {
        if (!JetBrainsPublication.isAndroidXGroup(id.group)) return
        val projectPath = JetBrainsPublication.projectPathForCoordinates(id.group, id.name) ?: return

        // Do not customize capabilities for not published artifacts
        if (!JetBrainsPublication.shouldPublish(projectPath)) return

        // Add capability with a common resolver group to enable conflict resolution
        allVariants { variant ->
            variant.withCapabilities {
                // Use implicit declaration
            }
        }
    }
}

/**
 * Gradle component metadata rule that adds capabilities to artifacts with
 * org.jetbrains.androidx.* or org.jetbrains.compose.* groups.
 *
 * This enables Gradle's capability-based conflict resolution to identify these artifacts
 * as providing the same functionality as their original androidx.* counterparts,
 * allowing the resolution strategy to choose the preferred version.
 */
private class AndroidXCapabilityRule : ComponentMetadataRule {
    override fun execute(context: ComponentMetadataContext): Unit = context.details.run {
        if (!JetBrainsPublication.isJetBrainsForkGroup(id.group)) return

        // Add capability with a common resolver group to enable conflict resolution
        allVariants { variant ->
            variant.withCapabilities {
                // Use implicit declaration
            }
        }
    }
}

/**
 * Configures capability resolution for JetBrains and AndroidX projects in a Gradle build.
 * It ensures correct dependency resolution by handling conflicts between `androidx.*` and `org.jetbrains.*` artifacts.
 */
fun Project.configureJetBrainsCapabilityResolution() {
    // Register the component metadata rule globally for external dependencies
    dependencies.components.all(
        if (isJetBrainsFork(this)) {
            JetBrainsCapabilityRule::class.java
        } else {
            AndroidXCapabilityRule::class.java
        }
    )

    // Configure capability resolution for all projects
    configurations.configureEach { configuration ->

        // https://github.com/gradle/gradle/issues/35943 workaround
        if (path.contains("integration-tests") || path.contains("samples")) {
            configuration.resolutionStrategy.dependencySubstitution {
                it.substitute(it.module("androidx.compose.ui:ui"))
                    .using(it.project(":compose:ui:ui"))
            }
        }

        configuration.resolutionStrategy.capabilitiesResolution.all { details ->
            if (JetBrainsPublication.isAndroidXGroup(details.capability.group)) {
                details.selectPreferredAndroidXCandidate()
            }
        }
    }
}

// TODO CMP-10368 fix old capability mechanism after migration to new artifact redirection
data class ArtifactRedirection(
    val groupId: String,
    val defaultVersion: String,
    val targetNames: Set<String>,
    val targetVersions: Map<String, String> = emptyMap()
) {
    fun versionForTargetOrDefault(targetName: String): String {
        return targetVersions[targetName.lowercase()] ?: defaultVersion
    }

    fun versionForConfigurationOrDefault(configurationName: String): String {
        // Configuration names are target-prefixed in Kotlin KMP publications, for example:
        // "desktopApiElements" or "iosArm64MetadataElements".
        val targetName = targetVersions.keys.firstOrNull {
            configurationName.startsWith(it, ignoreCase = true)
        }
        return versionForTargetOrDefault(targetName ?: "")
    }
}

fun Project.artifactRedirection(): ArtifactRedirection? {
    val mpe = extensions.findByType(AndroidXMultiplatformExtension::class.java) ?: return null
    val decls = mpe.redirectTargetDecls
    if (decls.isEmpty()) return null
    val groupId = decls.map { it.redirectCoordinate.group }.distinct().singleOrNull() ?: return null
    val defaultVersion = decls.firstNotNullOfOrNull {
        it.redirectCoordinate.version ?: findArtifactRedirectionVersion(it.redirectCoordinate.group)
    } ?: return null
    val targetNames = decls.map { it.targetName.lowercase() }.toSet()
    return ArtifactRedirection(
        groupId = groupId,
        defaultVersion = defaultVersion,
        targetNames = targetNames,
    )
}

// TODO CMP-10368 fix old capability mechanism after migration to new artifact redirection
fun Project.configureRedirectionCapability() {
//    // Compatibility stubs already wrap androidx artifacts directly; adding extra outgoing
//    // redirection capability here can break IDE metadata resolution for stubbed KMP modules.
//    if (JetBrainsPublication.isCompatibilityStubProject(this)) return
    if (!JetBrainsPublication.shouldPublish(this)) return
    val redirection = artifactRedirection() ?: return
    if (redirection.targetNames.isEmpty()) return

    // Configure resolution strategy to handle all capability conflicts
    configurations.configureEach { configuration ->
        if (configuration.isCanBeConsumed) {
            // It's important to declare the implicit capability explicitly because once you define
            // any explicit capability, all capabilities must be declared, including the implicit one.
            configuration.outgoing.capability("$group:$name:$version")

            // Add the androidx.* capability in addition to the implicit project capability
            val redirectedVersion = redirection.versionForConfigurationOrDefault(configuration.name)
            configuration.outgoing.capability("${redirection.groupId}:$name:$redirectedVersion")
        }
    }
}

internal fun Project.publishedRedirectionCapabilities(): Set<String> {
    val redirection = artifactRedirection() ?: return emptySet()
    if (redirection.targetNames.isEmpty()) return emptySet()

    return buildSet {
        add("$group:$name:$version")
        add("${redirection.groupId}:$name:${redirection.defaultVersion}")
        redirection.targetVersions.values.forEach { redirectedVersion ->
            add("${redirection.groupId}:$name:$redirectedVersion")
        }
    }
}

private fun CapabilityResolutionDetails.selectPreferredAndroidXCandidate() {
    // Only intervene if there are multiple candidates
    if (candidates.size <= 1) {
        return
    }

    // Priority order: 1) Project reference, 2) org.jetbrains.*, 3) androidx.*
    val projectCandidate = candidates.firstOrNull { candidate ->
        candidate.id is ProjectComponentIdentifier
    }
    if (projectCandidate != null) {
        // Project reference always wins over external dependencies
        select(projectCandidate)
        return
    }
    
    // Prefer org.jetbrains.* over androidx.*
    val jetBrainsCandidate = candidates.firstOrNull { candidate ->
        val candidateId = candidate.id
        if (candidateId is ModuleComponentIdentifier) {
            JetBrainsPublication.isJetBrainsForkGroup(candidateId.group)
        } else {
            false
        }
    }
    if (jetBrainsCandidate != null) {
        select(jetBrainsCandidate)
        return
    }

    // Let Gradle use its default resolution
}
