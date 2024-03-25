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

package androidx.build

import androidx.build.jetbrains.artifactRedirecting
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.create
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinMultiplatformPluginWrapper
import org.jetbrains.kotlin.gradle.plugin.mpp.AbstractKotlinTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinAndroidTarget
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
        val comp = compilations.getByName("main")
        val androidAnnotationVersion =
            project.findProperty("artifactRedirecting.androidx.annotation.version")!!
        val androidCollectionVersion =
            project.findProperty("artifactRedirecting.androidx.collection.version")!!
        val androidLifecycleVersion =
            project.findProperty("artifactRedirecting.androidx.lifecycle.version")!!
        listOf(
            comp.configurations.compileDependencyConfiguration,
            comp.configurations.runtimeDependencyConfiguration,
            comp.configurations.apiConfiguration,
            comp.configurations.implementationConfiguration,
            comp.configurations.runtimeOnlyConfiguration,
            comp.configurations.compileOnlyConfiguration,
        ).forEach { c ->
            c?.resolutionStrategy {

                // TODO: It should be based on config
                it.dependencySubstitution {
                    it.substitute(it.project(":annotation:annotation"))
                        .using(it.module("androidx.annotation:annotation:$androidAnnotationVersion"))
                    it.substitute(it.project(":collection:collection"))
                        .using(it.module("androidx.collection:collection:$androidCollectionVersion"))
                    it.substitute(it.project(":lifecycle:lifecycle-common"))
                        .using(it.module("androidx.lifecycle:lifecycle-common:$androidLifecycleVersion"))
                    it.substitute(it.project(":lifecycle:lifecycle-runtime"))
                        .using(it.module("androidx.lifecycle:lifecycle-runtime:$androidLifecycleVersion"))
                    it.substitute(it.project(":lifecycle:lifecycle-viewmodel"))
                        .using(it.module("androidx.lifecycle:lifecycle-viewmodel:$androidLifecycleVersion"))
                }
            }
        }
    }

}
class JetbrainsAndroidXPlugin : Plugin<Project> {

    @Suppress("UNREACHABLE_CODE", "UNUSED_VARIABLE")
    override fun apply(project: Project) {
        project.plugins.all { plugin ->
            if (plugin is KotlinMultiplatformPluginWrapper) {
                onKotlinMultiplatformPluginApplied(project)
            }
        }
    }

    private fun onKotlinMultiplatformPluginApplied(project: Project) {
        enableArtifactRedirectingPublishing(project)
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

    companion object {

        @Suppress("UNUSED_PARAMETER")
        @JvmStatic
        fun applyAndConfigure(
            project: Project
        ) {}
    }
}

private val Project.multiplatformExtension
    get() = extensions.findByType(KotlinMultiplatformExtension::class.java)

fun Project.experimentalArtifactRedirectingPublication() : Boolean = findProperty("artifactRedirecting.publication") == "true"
fun Project.artifactRedirectingAndroidxVersion() : String? = findProperty("artifactRedirecting.androidx.compose.version") as String?
fun Project.artifactRedirectingAndroidxFoundationVersion() : String? = findProperty("artifactRedirecting.androidx.compose.foundation.version") as String?
fun Project.artifactRedirectingAndroidxMaterial3Version() : String? = findProperty("artifactRedirecting.androidx.compose.material3.version") as String?
fun Project.artifactRedirectingAndroidxMaterialVersion() : String? = findProperty("artifactRedirecting.androidx.compose.material.version") as String?

fun enableArtifactRedirectingPublishing(project: Project) {
    if (!project.experimentalArtifactRedirectingPublication()) return

    if (project.experimentalArtifactRedirectingPublication() && (project.artifactRedirectingAndroidxVersion() == null)) {
        error("androidx version should be specified for OEL publications")
    }

    val ext = project.multiplatformExtension ?: error("expected a multiplatform project")

    val redirecting = project.artifactRedirecting()
    val newRootComponent: CustomRootComponent = run {
        val rootComponent = project
            .components
            .withType(KotlinSoftwareComponentWithCoordinatesAndPublication::class.java)
            .getByName("kotlin")

        CustomRootComponent(rootComponent) { configuration ->
            val targetName = redirecting.targetVersions.keys.firstOrNull {
                // we rely on the fact that configuration name starts with target name
                configuration.name.startsWith(it)
            }
            val targetVersion = redirecting.versionForTargetOrDefault(targetName ?: "")
            project.dependencies.create(
                redirecting.groupId, project.name, targetVersion
            )
        }
    }

    val oelTargetNames = (project.findProperty("artifactRedirecting.publication.targetNames") as? String ?: "")
        .split(",").toSet()

    ext.targets.all { target ->
        if (target.name in oelTargetNames || target is KotlinAndroidTarget) {
            project.publishAndroidxReference(target as AbstractKotlinTarget, newRootComponent)
        }
    }
}