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

package androidx.build

import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.plugin.mpp.*
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.internal.publication.DefaultMavenPublication
import org.gradle.api.attributes.Usage
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.DependencyConstraint
import org.gradle.api.artifacts.ExcludeRule
import org.gradle.api.artifacts.ModuleDependency
import org.gradle.api.artifacts.ModuleVersionIdentifier
import org.gradle.api.artifacts.PublishArtifact
import org.gradle.api.attributes.AttributeContainer
import org.gradle.api.capabilities.Capability
import org.gradle.api.component.ComponentWithCoordinates
import org.gradle.api.component.ComponentWithVariants
import org.gradle.api.component.SoftwareComponent
import org.gradle.api.internal.component.SoftwareComponentInternal
import org.gradle.api.internal.component.UsageContext
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.api.publish.maven.tasks.AbstractPublishToMaven
import org.jetbrains.kotlin.gradle.InternalKotlinGradlePluginApi

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
    override fun getUsage(): Usage = error("Should not be accessed!")
}

@OptIn(InternalKotlinGradlePluginApi::class)
internal fun Project.publishAndroidxReference(target: AbstractKotlinTarget, newRootComponent: CustomRootComponent) {
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

        target.kotlinComponents.forEach { component ->
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
                else -> emptyList()
            }

            usages.forEach { usage ->
                // Use -published configuration because it would have correct attribute set
                // required for publication.
                val configurationName = usage.name + "-published"
                configurations.matching { it.name == configurationName }.all { conf ->
                    newRootComponent.addUsageFromConfiguration(conf)
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
    override fun getVariants(): Set<SoftwareComponent> = rootComponent.variants
    override fun getCoordinates(): ModuleVersionIdentifier =
        rootComponent.coordinates

    override fun getUsages(): Set<UsageContext> = rootComponent.usages + extraUsages

    private val extraUsages = mutableSetOf<UsageContext>()

    fun addUsageFromConfiguration(configuration: Configuration) {
        val newDependency = customizeDependencyPerConfiguration(configuration)

        extraUsages.add(
            CustomUsage(
                name = configuration.name,
                attributes = configuration.attributes,
                dependencies = setOf(newDependency)
            )
        )
    }
}