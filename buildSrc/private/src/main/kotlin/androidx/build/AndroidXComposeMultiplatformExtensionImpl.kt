/*
 * Copyright 2021 The Android Open Source Project
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

import javax.inject.Inject
import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinJsCompilerType
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import org.jetbrains.kotlin.gradle.plugin.*
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinAndroidTarget
import org.gradle.api.Action
import org.jetbrains.kotlin.gradle.plugin.mpp.*
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.internal.publication.DefaultMavenPublication
import org.gradle.api.attributes.Usage
import org.gradle.api.tasks.Copy
import org.gradle.kotlin.dsl.creating
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.getValue
import org.jetbrains.kotlin.konan.target.KonanTarget
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
import org.gradle.kotlin.dsl.create
import org.jetbrains.kotlin.gradle.targets.js.dsl.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.targets.js.testing.KotlinJsTest
import org.tomlj.Toml

open class AndroidXComposeMultiplatformExtensionImpl @Inject constructor(
    val project: Project
) : AndroidXComposeMultiplatformExtension() {
    private val multiplatformExtension =
        project.extensions.getByType(KotlinMultiplatformExtension::class.java)

    private val skikoVersion: String

    init {
        val toml = Toml.parse(
            project.rootProject.projectDir.resolve("gradle/libs.versions.toml").toPath()
        )
        skikoVersion = toml.getTable("versions")!!.getString("skiko")!!
        println("Skiko version = $skikoVersion")
    }

    override fun android(): Unit = multiplatformExtension.run {
        androidTarget()

        val androidMain = sourceSets.getByName("androidMain")
        val jvmMain = getOrCreateJvmMain()
        androidMain.dependsOn(jvmMain)

        val androidTest = sourceSets.getByName("androidUnitTest")
        val jvmTest = getOrCreateJvmTest()
        androidTest.dependsOn(jvmTest)
    }

    override fun desktop(): Unit = multiplatformExtension.run {
        jvm("desktop")

        val desktopMain = sourceSets.getByName("desktopMain")
        val jvmMain = getOrCreateJvmMain()
        desktopMain.dependsOn(jvmMain)

        val desktopTest = sourceSets.getByName("desktopTest")
        val jvmTest = getOrCreateJvmTest()
        desktopTest.dependsOn(jvmTest)
    }

    override fun js(): Unit = multiplatformExtension.run {
        js(KotlinJsCompilerType.IR) {
            browser()
        }

        val commonMain = sourceSets.getByName("commonMain")
        val jsMain = sourceSets.getByName("jsMain")
        jsMain.dependsOn(commonMain)
    }

    @OptIn(ExperimentalWasmDsl::class)
    override fun wasm(): Unit = multiplatformExtension.run {
        wasmJs {
            browser {
                testTask(Action<KotlinJsTest> {
                    it.useKarma {
                        useChromeHeadless()
                        useConfigDirectory(
                            project.rootProject.projectDir.resolve("mpp/karma.config.d/wasm")
                        )
                    }
                })
            }
        }

        val resourcesDir = "${project.buildDir}/resources"
        val skikoWasm by project.configurations.creating

        // Below code helps configure the tests for k/wasm targets
        project.dependencies {
            skikoWasm("org.jetbrains.skiko:skiko-js-wasm-runtime:${skikoVersion}")
        }

        val unzipTask = project.tasks.register("unzipSkikoForKWasm", Copy::class.java) {
            it.destinationDir = project.file(resourcesDir)
            it.from(skikoWasm.map { project.zipTree(it) })
        }

        val loadTestsTask = project.tasks.register("loadTests", Copy::class.java) {
            it.destinationDir = project.file(resourcesDir)
            it.from(
                project.rootProject.projectDir.resolve(
                    "mpp/load-wasm-tests/load-test-template.mjs"
                )
            )
            it.filter {
                it.replace("{module-name}", getDashedProjectName())
            }
        }

        project.tasks.getByName("wasmJsTestProcessResources").apply {
            dependsOn(loadTestsTask)
        }

        project.tasks.getByName("wasmJsBrowserTest").apply {
            dependsOn(unzipTask)
        }

        val commonMain = sourceSets.getByName("commonMain")
        val wasmMain = sourceSets.getByName("wasmJsMain")
        wasmMain.dependsOn(commonMain)

        sourceSets.getByName("wasmJsTest").also {
            it.resources.setSrcDirs(it.resources.srcDirs)
            it.resources.srcDirs(unzipTask.map { it.destinationDir })
        }
    }

    private fun getDashedProjectName(p: Project = project): String {
        if (p == project.rootProject) {
            return p.name
        }
        return getDashedProjectName(p = p.parent!!) + "-" + p.name
    }

    override fun darwin(): Unit = multiplatformExtension.run {
        macosX64()
        macosArm64()
        iosX64("uikitX64")
        iosArm64("uikitArm64")
        iosSimulatorArm64("uikitSimArm64")

        val commonMain = sourceSets.getByName("commonMain")
        val nativeMain = sourceSets.create("nativeMain")
        val darwinMain = sourceSets.create("darwinMain")
        val macosMain = sourceSets.create("macosMain")
        val macosX64Main = sourceSets.getByName("macosX64Main")
        val macosArm64Main = sourceSets.getByName("macosArm64Main")
        val uikitMain = sourceSets.create("uikitMain")
        val uikitX64Main = sourceSets.getByName("uikitX64Main")
        val uikitArm64Main = sourceSets.getByName("uikitArm64Main")
        val uikitSimArm64Main = sourceSets.getByName("uikitSimArm64Main")
        nativeMain.dependsOn(commonMain)
        darwinMain.dependsOn(nativeMain)
        macosMain.dependsOn(darwinMain)
        macosX64Main.dependsOn(macosMain)
        macosArm64Main.dependsOn(macosMain)
        uikitMain.dependsOn(darwinMain)
        uikitX64Main.dependsOn(uikitMain)
        uikitArm64Main.dependsOn(uikitMain)
        uikitSimArm64Main.dependsOn(uikitMain)

        val commonTest = sourceSets.getByName("commonTest")
        val nativeTest = sourceSets.create("nativeTest")
        val darwinTest = sourceSets.create("darwinTest")
        val macosTest = sourceSets.create("macosTest")
        val macosX64Test = sourceSets.getByName("macosX64Test")
        val macosArm64Test = sourceSets.getByName("macosArm64Test")
        val uikitTest = sourceSets.create("uikitTest")
        val uikitX64Test = sourceSets.getByName("uikitX64Test")
        val uikitArm64Test = sourceSets.getByName("uikitArm64Test")
        val uikitSimArm64Test = sourceSets.getByName("uikitSimArm64Test")
        nativeTest.dependsOn(commonTest)
        darwinTest.dependsOn(nativeTest)
        macosTest.dependsOn(darwinTest)
        macosX64Test.dependsOn(macosTest)
        macosArm64Test.dependsOn(macosTest)
        uikitTest.dependsOn(darwinTest)
        uikitX64Test.dependsOn(uikitTest)
        uikitArm64Test.dependsOn(uikitTest)
        uikitSimArm64Test.dependsOn(uikitTest)
    }

    private fun getOrCreateJvmMain(): KotlinSourceSet =
        getOrCreateSourceSet("jvmMain", "commonMain")

    private fun getOrCreateJvmTest(): KotlinSourceSet =
        getOrCreateSourceSet("jvmTest", "commonTest")

    private fun getOrCreateSourceSet(
        name: String,
        dependsOnSourceSetName: String
    ): KotlinSourceSet = multiplatformExtension.run {
        sourceSets.findByName(name)
            ?: sourceSets.create(name).apply {
                    dependsOn(sourceSets.getByName(dependsOnSourceSetName))
            }
    }

    private fun addUtilDirectory(vararg sourceSetNames: String) = multiplatformExtension.run {
        sourceSetNames.forEach { name ->
            val sourceSet = sourceSets.findByName(name)
            sourceSet?.let {
                it.kotlin.srcDirs(project.rootProject.files("compose/util/util/src/$name/kotlin/"))
            }
        }
    }

    override fun configureDarwinFlags() {
        val darwinFlags = listOf(
            "-linker-option", "-framework", "-linker-option", "Metal",
            "-linker-option", "-framework", "-linker-option", "CoreText",
            "-linker-option", "-framework", "-linker-option", "CoreGraphics",
            "-linker-option", "-framework", "-linker-option", "CoreServices"
        )
        val iosFlags = listOf("-linker-option", "-framework", "-linker-option", "UIKit")

        fun KotlinNativeTarget.configureFreeCompilerArgs() {
            val isIOS = konanTarget == KonanTarget.IOS_X64 ||
                konanTarget == KonanTarget.IOS_SIMULATOR_ARM64 ||
                konanTarget == KonanTarget.IOS_ARM64

            binaries.forEach {
                val flags = mutableListOf<String>().apply {
                    addAll(darwinFlags)
                    if (isIOS) addAll(iosFlags)
                }
                it.freeCompilerArgs = it.freeCompilerArgs + flags
            }
        }
        multiplatformExtension.run {
            macosX64 { configureFreeCompilerArgs() }
            macosArm64 { configureFreeCompilerArgs() }
            iosX64("uikitX64") { configureFreeCompilerArgs() }
            iosArm64("uikitArm64") { configureFreeCompilerArgs() }
            iosSimulatorArm64("uikitSimArm64") { configureFreeCompilerArgs() }
        }
    }
}

fun Project.experimentalOELPublication() : Boolean = findProperty("oel.publication") == "true"
fun Project.oelAndroidxVersion() : String? = findProperty("oel.androidx.version") as String?
fun Project.oelAndroidxFoundationVersion() : String? = findProperty("oel.androidx.foundation.version") as String?
fun Project.oelAndroidxMaterial3Version() : String? = findProperty("oel.androidx.material3.version") as String?
fun Project.oelAndroidxMaterialVersion() : String? = findProperty("oel.androidx.material.version") as String?

fun enableOELPublishing(project: Project) {
    if (!project.experimentalOELPublication()) return

    if (project.experimentalOELPublication() && (project.oelAndroidxVersion() == null)) {
        error("androidx version should be specified for OEL publications")
    }

    val ext = project.multiplatformExtension ?: error("expected a multiplatform project")

    ext.targets.all { target ->
        if (target is KotlinAndroidTarget) {
            project.publishAndroidxReference(target)
        }
    }
}


/**
 * Usage that should be added to rootSoftwareComponent to represent android-specific variants
 * It will be serialized to *.module in "variants" collection.
 */
private class CustomAndroidUsage(
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

private fun Project.publishAndroidxReference(target: KotlinAndroidTarget) {
    afterEvaluate {
        // Take root component which should contain "variants" (aka usages)
        // this component gets published as "main/common" module
        // that we want to add as android ones.
        val rootComponent = target.project
            .components
            .withType(KotlinSoftwareComponentWithCoordinatesAndPublication::class.java)
            .getByName("kotlin")

        val composeVersion = requireNotNull(target.project.oelAndroidxVersion()) {
            "Please specify oel.androidx.version property"
        }
        val material3Version =
            requireNotNull(target.project.oelAndroidxMaterial3Version()) {
                "Please specify oel.androidx.material3.version property"
            }
        val foundationVersion =
            target.project.oelAndroidxFoundationVersion() ?: composeVersion
        val materialVersion =
            target.project.oelAndroidxMaterialVersion() ?: composeVersion

        val groupId = target.project.group.toString()
        val version = if (groupId.contains("org.jetbrains.compose.material3")) {
            material3Version
        } else if (groupId.contains("org.jetbrains.compose.foundation")) {
            foundationVersion
        } else if (groupId.contains("org.jetbrains.compose.material")) {
            materialVersion
        } else {
            composeVersion
        }
        val dependencyGroup = target.project.group.toString().replace(
            "org.jetbrains.compose",
            "androidx.compose"
        )
        val newDependency = target.project.dependencies.create(dependencyGroup, name, version)


        // We can't add more usages to rootComponent, so we must decorate it
        val newRootComponent = object :
            SoftwareComponentInternal,
            ComponentWithVariants,
            ComponentWithCoordinates {
            override fun getName(): String = "kotlinDecoratedRootComponent"
            override fun getVariants(): Set<SoftwareComponent> = rootComponent.variants
            override fun getCoordinates(): ModuleVersionIdentifier =
                rootComponent.coordinates

            override fun getUsages(): Set<UsageContext> = rootComponent.usages + extraUsages

            private val extraUsages = mutableSetOf<UsageContext>()

            fun addUsageFromConfiguration(configuration: Configuration) {
                extraUsages.add(
                    CustomAndroidUsage(
                        name = configuration.name,
                        attributes = configuration.attributes,
                        dependencies = setOf(newDependency)
                    )
                )
            }
        }

        extensions.getByType(PublishingExtension::class.java).apply {
            val kotlinMultiplatform = publications
                .getByName("kotlinMultiplatform") as MavenPublication

            publications.create("kotlinMultiplatformDecorated", MavenPublication::class.java) {
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