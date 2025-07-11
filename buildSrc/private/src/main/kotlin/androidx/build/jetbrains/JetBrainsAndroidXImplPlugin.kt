/*
 * Copyright 2024 The Android Open Source Project
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

@file:Suppress("unused")

package androidx.build.jetbrains

import androidx.build.AndroidXExtension
import androidx.build.multiplatformExtension
import kotlinx.validation.ApiValidationExtension
import kotlinx.validation.ExperimentalBCVApi
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.create
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinMultiplatformPluginWrapper
import org.jetbrains.kotlin.gradle.plugin.mpp.AbstractKotlinTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinSoftwareComponentWithCoordinatesAndPublication
import org.jetbrains.kotlin.konan.target.KonanTarget

open class JetbrainsExtensions(
    val project: Project,
    val multiplatformExtension: KotlinMultiplatformExtension
) {

    // check for example here: https://maven.google.com/web/index.html?q=lifecyc#androidx.lifecycle
    val defaultKonanTargetsPublishedByAndroidx = setOf(
        KonanTarget.LINUX_X64,
        KonanTarget.IOS_X64,
        KonanTarget.IOS_ARM64,
        KonanTarget.IOS_SIMULATOR_ARM64,
        KonanTarget.MACOS_X64,
        KonanTarget.MACOS_ARM64,
    )

    @JvmOverloads
    fun configureKNativeRedirectingDependenciesInKlibManifest(
        konanTargets: Set<KonanTarget> = defaultKonanTargetsPublishedByAndroidx
    ) {
        multiplatformExtension.targets.all {
            if (it is KotlinNativeTarget && it.konanTarget in konanTargets) {
                it.substituteForRedirectedPublishedDependencies()
            }
        }
    }

    /**
     * When https://youtrack.jetbrains.com/issue/KT-61096 is implemented,
     * this workaround won't be needed anymore:
     *
     * K/Native stores the dependencies in klib manifest and tries to resolve them during compilation.
     * Since we use project dependency - implementation(project(...)), the klib manifest will reference
     * our groupId (for example org.jetbrains.compose.collection-internal instead of androidx.collection).
     * Therefore, the dependency can't be resolved since we don't publish libs for some k/native targets.
     *
     * To workaround that, we need to make sure
     * that the project dependency is substituted by a module dependency (from androidx).
     * We do this here. It should be called only for those k/native targets which require
     * redirection to androidx artefacts.
     *
     * For available androidx targets see:
     * https://maven.google.com/web/index.html#androidx.annotation
     * https://maven.google.com/web/index.html#androidx.collection
     * https://maven.google.com/web/index.html#androidx.lifecycle
     */
    fun KotlinNativeTarget.substituteForRedirectedPublishedDependencies() {
        val main = compilations.getByName("main")
        val test = compilations.getByName("test")
        val kNativeManifestRedirectingModulesRaw =
            project.property("artifactRedirection.modulesForKNativeManifest") as String

        val projectPathToRedirectingVersionMap = kNativeManifestRedirectingModulesRaw
            .split(",").associate {
                val pair = it.split("=")
                pair[0] to project.property(pair[1]) as String
            }
        listOf(main, test).flatMap {
            val configurations = it.configurations
            listOf(
                configurations.compileDependencyConfiguration,
                configurations.runtimeDependencyConfiguration,
                configurations.apiConfiguration,
                configurations.implementationConfiguration,
                configurations.runtimeOnlyConfiguration,
                configurations.compileOnlyConfiguration
            )
        }.forEach { c ->
            c?.resolutionStrategy {
                it.dependencySubstitution {
                    projectPathToRedirectingVersionMap.forEach { path, version ->
                        val pathElements = path.split(":").filter { it.isNotEmpty() }
                        val group = pathElements.dropLast(1).joinToString(".")
                        val module = pathElements.last()
                        val artifact = "androidx.$group:$module:$version"
                        it.substitute(it.project(path)).using(it.module(artifact))
                    }
                }
            }
        }
    }

}
class JetBrainsAndroidXImplPlugin : Plugin<Project> {

    @Suppress("UNREACHABLE_CODE", "UNUSED_VARIABLE")
    override fun apply(project: Project) {
        project.plugins.all { plugin ->
            if (plugin is KotlinMultiplatformPluginWrapper) {
                onKotlinMultiplatformPluginApplied(project)
            }
        }
    }

    private fun onKotlinMultiplatformPluginApplied(project: Project) {
        enableArtifactRedirectionPublishing(project)
        enableBinaryCompatibilityValidator(project)
        val multiplatformExtension =
            project.extensions.getByType(KotlinMultiplatformExtension::class.java)

        val extension = project.extensions.create<JetbrainsExtensions>(
            "jetbrainsExtension",
            project,
            multiplatformExtension
        )

        // Note: Currently we call it unconditionally since Androidx provides the same set of
        // Konan targets for all multiplatform libs they publish.
        // In the future we might need to call it with non-default konan targets set in some modules
        extension.configureKNativeRedirectingDependenciesInKlibManifest()
    }
}

private fun enableArtifactRedirectionPublishing(project: Project) {
    val redirection = project.artifactRedirection() ?: return

    val ext = project.multiplatformExtension ?: error("expected a multiplatform project")

    val newRootComponent: CustomRootComponent = run {
        val rootComponent = project
            .components
            .withType(KotlinSoftwareComponentWithCoordinatesAndPublication::class.java)
            .getByName("kotlin")

        CustomRootComponent(rootComponent) { configuration ->
            val targetName = redirection.targetVersions.keys.firstOrNull {
                // we rely on the fact that configuration name starts with target name
                configuration.name.startsWith(it, ignoreCase = true)
            }
            val targetVersion = redirection.versionForTargetOrDefault(targetName ?: "")
            project.dependencies.create(
                redirection.groupId, project.name, targetVersion
            )
        }
    }

    ext.targets.all { target ->
        if (target.name.lowercase() in redirection.targetNames) {
            project.publishAndroidxReference(target as AbstractKotlinTarget, newRootComponent)
        }
    }
}

@OptIn(ExperimentalBCVApi::class)
private fun enableBinaryCompatibilityValidator(project: Project) {
    val androidXExtension = project.extensions.findByType(AndroidXExtension::class.java)
        ?: throw Exception("You have applied AndroidXComposePlugin without AndroidXPlugin")
    project.afterEvaluate {
        if (androidXExtension.shouldPublish()) {
            project.apply(plugin = "org.jetbrains.kotlinx.binary-compatibility-validator")
            project.extensions.getByType(ApiValidationExtension::class.java).apply {
                klib.enabled = true
                nonPublicMarkers += NON_PUBLIC_MARKERS
            }
        }
    }
}

// Not ideal to have a list instead of a pattern to match but this is all the API supports right now
// https://github.com/Kotlin/binary-compatibility-validator/issues/280
private val NON_PUBLIC_MARKERS =
    setOf(
        "androidx.annotation.Experimental",
        "androidx.compose.animation.ExperimentalAnimationApi",
        "androidx.compose.animation.ExperimentalSharedTransitionApi",
        "androidx.compose.animation.core.ExperimentalAnimatableApi",
        "androidx.compose.animation.core.ExperimentalAnimationSpecApi",
        "androidx.compose.animation.core.ExperimentalTransitionApi",
        "androidx.compose.animation.core.InternalAnimationApi",
        "androidx.compose.animation.graphics.ExperimentalAnimationGraphicsApi",
        "androidx.compose.foundation.gestures.ExperimentalTapGestureDetectorBehaviorApi",
        "androidx.compose.foundation.ExperimentalFoundationApi",
        "androidx.compose.foundation.InternalFoundationApi",
        "androidx.compose.foundation.layout.ExperimentalLayoutApi",
        "androidx.compose.material.ExperimentalMaterialApi",
        "androidx.compose.runtime.ExperimentalComposeApi",
        "androidx.compose.runtime.ExperimentalComposeRuntimeApi",
        "androidx.compose.runtime.InternalComposeApi",
        "androidx.compose.runtime.InternalComposeTracingApi",
        "androidx.compose.ui.ExperimentalComposeUiApi",
        "androidx.compose.ui.InternalComposeUiApi",
        "androidx.compose.ui.input.pointer.util.ExperimentalVelocityTrackerApi",
        "androidx.compose.ui.node.InternalCoreApi",
        "androidx.compose.ui.test.ExperimentalTestApi",
        "androidx.compose.ui.test.InternalTestApi",
        "androidx.compose.ui.text.ExperimentalTextApi",
        "androidx.compose.ui.text.InternalTextApi",
        "androidx.compose.ui.unit.ExperimentalUnitApi",
        "androidx.lifecycle.viewmodel.compose.SavedStateHandleSaveableApi",
        "androidx.window.core.ExperimentalWindowApi",
    )
