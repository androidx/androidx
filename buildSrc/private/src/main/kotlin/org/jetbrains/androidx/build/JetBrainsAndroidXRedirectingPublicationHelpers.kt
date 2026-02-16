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

package org.jetbrains.androidx.build

import com.android.utils.mapValuesNotNull
import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.plugin.mpp.*
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.internal.publication.DefaultMavenPublication
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.DependencyConstraint
import org.gradle.api.artifacts.ExcludeRule
import org.gradle.api.artifacts.ModuleDependency
import org.gradle.api.artifacts.ModuleIdentifier
import org.gradle.api.artifacts.ModuleVersionIdentifier
import org.gradle.api.artifacts.PublishArtifact
import org.gradle.api.artifacts.ResolvedDependency
import org.gradle.api.attributes.AttributeContainer
import org.gradle.api.capabilities.Capability
import org.gradle.api.component.ComponentWithCoordinates
import org.gradle.api.component.ComponentWithVariants
import org.gradle.api.component.SoftwareComponent
import org.gradle.api.internal.artifacts.DefaultModuleIdentifier
import org.gradle.api.internal.artifacts.DefaultModuleVersionIdentifier
import org.gradle.api.internal.component.SoftwareComponentInternal
import org.gradle.api.internal.component.UsageContext
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.publish.maven.tasks.AbstractPublishToMaven
import org.jetbrains.kotlin.gradle.ExternalKotlinTargetApi
import org.jetbrains.kotlin.gradle.InternalKotlinGradlePluginApi
import org.jetbrains.kotlin.gradle.plugin.KotlinTargetComponent
import org.jetbrains.kotlin.gradle.plugin.mpp.external.DecoratedExternalKotlinTarget

/**
 * Usage that should be added to rootSoftwareComponent to represent target-specific variants
 * It will be serialized to *.module in "variants" collection.
 */
internal class CustomUsage(
    private val name: String,
    private val attributes: AttributeContainer,
    private val dependencies: Set<ModuleDependency>
) : UsageContext {
    override fun getName(): String = name
    override fun getArtifacts(): Set<PublishArtifact> = emptySet()
    override fun getAttributes(): AttributeContainer = attributes
    override fun getCapabilities(): Set<Capability> = emptySet()
    override fun getDependencies(): Set<ModuleDependency> = dependencies
    override fun getDependencyConstraints(): Set<DependencyConstraint> = emptySet()
    override fun getGlobalExcludes(): Set<ExcludeRule> = emptySet()
}

@OptIn(InternalKotlinGradlePluginApi::class, ExternalKotlinTargetApi::class)
internal fun Project.setupRedirection(target: DecoratedExternalKotlinTarget, newRootComponent: CustomRootComponent) {
    setupRedirection(target.name, target.kotlinComponents, newRootComponent)
}
@OptIn(InternalKotlinGradlePluginApi::class)
internal fun Project.setupRedirection(target: AbstractKotlinTarget, newRootComponent: CustomRootComponent) {
    setupRedirection(target.name, target.kotlinComponents, newRootComponent)
}

@OptIn(InternalKotlinGradlePluginApi::class)
internal fun Project.setupRedirection(targetName: String, kotlinComponents: Set<KotlinTargetComponent>, newRootComponent: CustomRootComponent) {
    afterEvaluate {
        extensions.getByType(PublishingExtension::class.java).apply {
            val kotlinMultiplatform = publications
                .getByName("kotlinMultiplatform") as MavenPublication

            publications.findByName("kotlinMultiplatformDecorated") ?: publications.create("kotlinMultiplatformDecorated", MavenPublication::class.java) {
                it.artifactId = kotlinMultiplatform.artifactId
                it.groupId = kotlinMultiplatform.groupId
                it.version = kotlinMultiplatform.version

                it.from(newRootComponent)
            }
        }

        // Disable all publication tasks that uses OLD rootSoftwareComponent: we don't want to
        // accidentally publish two "root" components
        tasks.withType(AbstractPublishToMaven::class.java).configureEach {
            if (it.publication.name == "kotlinMultiplatform") it.enabled = false
        }

        kotlinComponents.forEach { component ->
            val componentName = component.name

            if (component is KotlinVariant)
                component.publishable = false

            extensions.getByType(PublishingExtension::class.java)
                .publications.withType(DefaultMavenPublication::class.java)
                // isAlias is needed for Gradle to ignore the fact that there's a
                // publication that is not referenced as an available-at variant of the root module
                // and has the Maven coordinates that are different from those of the root module
                // FIXME: internal Gradle API! We would rather not create the publications,
                //        but some API for that is needed in the Kotlin Gradle plugin
                .all { publication ->
                    if (publication.name == componentName) {
                        publication.isAlias = true
                    }
                }

            val usages = when (component) {
                is KotlinVariant -> component.usages
                is KotlinVariantWithMetadataVariant -> component.usages
                is JointAndroidKotlinTargetComponent -> component.usages
                is InternalKotlinTargetComponent -> component.usages
                else -> emptyList()
            }

            usages.forEach { usage ->
                // Use -published configuration because it would have correct attribute set
                // required for publication.
                val configurationName = if (usage.name.endsWith("-published")) usage.name else usage.name + "-published"

                configurations.matching { it.name == configurationName }.all { conf ->
                    newRootComponent.replaceUsagesFor(targetName, conf, usage)
                }
            }
        }
    }
}

internal class CustomRootComponent(
    val rootComponent: KotlinSoftwareComponentWithCoordinatesAndPublication,
    val customizeDependencyPerConfiguration: (Configuration) -> ModuleDependency
) : SoftwareComponentInternal, ComponentWithVariants, ComponentWithCoordinates {
    override fun getName(): String = "kotlinDecoratedRootComponent"
    override fun getVariants(): Set<SoftwareComponent> =
        rootComponent.variants.filterTo(mutableSetOf()) { it.name !in replacedTargets }

    override fun getCoordinates(): ModuleVersionIdentifier =
        rootComponent.coordinates

    override fun getUsages(): Set<UsageContext> = rootComponent.usages + extraUsages.map { it() }

    private val replacedTargets = mutableSetOf<String>()
    private val extraUsages = mutableSetOf<() -> UsageContext>()

    fun replaceUsagesFor(targetName: String, configuration: Configuration, defaultUsage: KotlinUsageContext) {
        replacedTargets.add(targetName)
        extraUsages.add { usageFor(configuration, defaultUsage) }
    }

    private fun usageFor(configuration: Configuration, defaultUsage: KotlinUsageContext): CustomUsage {
        val newDependency = customizeDependencyPerConfiguration(configuration)

        // Dependencies from <target>Main
        val targetDependencies = defaultUsage.dependencies.toSet()

        // Dependencies from commonMain/skikoMain/webMain/etc
        val sharedSourcesetsDependencies = rootComponent.usages.flatMap { it.dependencies }

        // Intersection of the dependencies gives us commonMain deps
        val commonMainDependencies = sharedSourcesetsDependencies.filter { it in targetDependencies }

        return CustomUsage(
            name = configuration.name,
            attributes = configuration.attributes,
            dependencies = setOf(newDependency) + commonMainDependencies
        )
    }
}

internal fun Project.originalToRedirectedDependency(
    componentName: String
): Map<ModuleIdentifier, ModuleVersionIdentifier> {
    /**
     * Find a redirect to another group and version.
     *
     * Use heuristic method that compares modules names. Example:
     *   [first-level-dependency] org.jetbrains.androidx.lifecycle:lifecycle-runtime:2.8.4 ->
     *   [artifact-with-the-same-name] androidx.lifecycle:lifecycle-runtime:2.8.5 ->
     *   [artifact-with-the-same-name-plus-suffix] androidx.lifecycle:lifecycle-runtime-desktop:2.8.5
     *
     * The first dependency redirects to the last one.
     */
    fun ResolvedDependency.findRedirectedDependencyHeuristically() =
        children
            .find { it.moduleName == moduleName }
            ?.children
            // don't check `it.moduleName == "moduleName-$target"` here,
            // as it can be resolved to any other suitable target
            // (for example, to jvm, or any other custom)
            ?.find { it.moduleName.startsWith(moduleName) }

    /**
     * Extract redirections from project configuration
     *
     * Example for compose:ui
     * org.jetbrains.androidx.performance:performance-annotation-iosarm64=androidx.performance:performance-annotation-iosarm64:1.0.0-alpha01
     * org.jetbrains.androidx.performance:performance-annotation-jvm=androidx.performance:performance-annotation-jvm:1.0.0-alpha01
     * org.jetbrains.compose.annotation-internal:annotation-jvm=androidx.annotation:annotation-jvm:1.9.1
     * org.jetbrains.compose.collection-internal:collection-jvm=androidx.collection:collection-jvm:1.5.0-beta01
     * ...
     */
    val projectDefined =
        JetBrainsPublication.projectPathToLibrary.keys
            .mapNotNull { project.findProject(it) }
            .flatMap { project ->
                val redirecting = project.artifactRedirection() ?: return@flatMap emptyList()
                redirecting.targetNames.filter { it.isNotEmpty() }.map {
                    val group = project.group.toString()
                    val name = project.name
                    val target = it
                    val original = DefaultModuleIdentifier.newId(group, "$name-$target")
                    val redirected = DefaultModuleVersionIdentifier.newId(
                        redirecting.groupId,
                        "$name-$target",
                        redirecting.versionForTargetOrDefault(target)
                    )
                    original to redirected
                }
            }
            .associate { it }

    fun mainConfiguration() =
        configurations.find { it.name == "${componentName}RuntimeClasspath" } ?:
        configurations.find { it.name == "${componentName}CompileKlibraries" }!!

    /**
     * Extract redirections for dependencies using heuristic method (for both project, and external)
     *
     * Example for compose:ui
     * org.jetbrains.compose.annotation-internal:annotation=androidx.annotation:annotation-jvm:1.9.1
     * org.jetbrains.compose.collection-internal:collection=androidx.collection:collection-jvm:1.5.0-beta02
     * org.jetbrains.androidx.lifecycle:lifecycle-common=androidx.lifecycle:lifecycle-common-jvm:2.8.5
     * org.jetbrains.androidx.lifecycle:lifecycle-runtime=androidx.lifecycle:lifecycle-runtime-desktop:2.8.5
     * org.jetbrains.androidx.lifecycle:lifecycle-viewmodel=androidx.lifecycle:lifecycle-viewmodel-desktop:2.8.5
     *
     * It is workaround for
     * https://youtrack.jetbrains.com/issue/CMP-7764/Redirection-of-artifacts-breaks-poms-for-multiplatform-libraries-that-use-them
     * After it is resolved, externalWithHeuristic shouldn't be needed.
     */
    val externalWithHeuristic = mainConfiguration()
        .resolvedConfiguration
        .firstLevelModuleDependencies
        .orEmpty()
        .associateBy { DefaultModuleIdentifier.newId(it.moduleGroup, it.moduleName) }
        .mapValuesNotNull { it.value.findRedirectedDependencyHeuristically()?.module?.id }

    return projectDefined + externalWithHeuristic
}
