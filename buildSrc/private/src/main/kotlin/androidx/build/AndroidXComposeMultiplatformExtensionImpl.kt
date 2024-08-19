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

import java.io.ByteArrayOutputStream
import javax.inject.Inject
import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.api.attributes.Attribute
import org.gradle.api.tasks.Copy
import org.gradle.api.tasks.Exec
import org.gradle.kotlin.dsl.creating
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.getValue
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinJsCompilerType
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTargetWithSimulatorTests
import org.jetbrains.kotlin.gradle.targets.js.dsl.ExperimentalWasmDsl
import org.jetbrains.kotlin.gradle.targets.js.testing.KotlinJsTest
import org.jetbrains.kotlin.gradle.targets.native.DefaultSimulatorTestRun
import org.jetbrains.kotlin.konan.target.KonanTarget
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

    internal val Project.isInIdea: Boolean
        get() {
            return System.getProperty("idea.active")?.toBoolean() == true
        }

    @OptIn(ExperimentalWasmDsl::class)
    override fun wasm(): Unit = multiplatformExtension.run {
        wasmJs {
            browser {
                testTask(Action<KotlinJsTest> {
                    it.useKarma {
                        useChrome()
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

    override fun linuxX64(): Unit = multiplatformExtension.run {
        linuxX64()
    }

    override fun linuxArm64(): Unit = multiplatformExtension.run {
        linuxArm64()
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

    // https://youtrack.jetbrains.com/issue/KT-55751/MPP-Gradle-Consumable-configurations-must-have-unique-attributes
    private val instrumentedTestAttribute = Attribute.of("instrumentedTest", String::class.java)
    private val instrumentedTestCompilationAttribute = Attribute.of("instrumentedTestCompilation", String::class.java)

//    The consumer was configured to find a library for use during 'kotlin-metadata',
//    preferably optimized for non-jvm, as well as
//    attribute 'org.jetbrains.kotlin.platform.type'
//        with value 'native',
//    attribute 'org.jetbrains.kotlin.native.target'
//        with value 'ios_simulator_arm64',
//    attribute 'instrumentedTest'
//        with value 'Test'.
//    However we cannot choose between the following variants of project :compose:ui:ui:
//        - uikitInstrumentedSimArm64ApiElements
//        - uikitInstrumentedSimArm64MetadataElements
//        - uikitSimArm64ApiElements
//        - uikitSimArm64MetadataElements


    override fun iosInstrumentedTest(): Unit =
        multiplatformExtension.run {
            fun getDeviceName(): String? {
                return project.findProperty("iosSimulatorName") as? String
            }

            val bootTask = project.tasks.register("bootIosSimulator", Exec::class.java) { task ->
                task.isIgnoreExitValue = true
                task.errorOutput = ByteArrayOutputStream()
                task.doFirst {
                    val simulatorName = getDeviceName()
                        ?: error("Device is not provided. Use Use the -PiosSimulatorName=<Device name> flag to pass the device.")
                    task.commandLine("xcrun", "simctl", "boot", simulatorName)
                }
                task.doLast {
                    val result = task.executionResult.get()
                    if (result.exitValue != 148 && result.exitValue != 149) { // ignoring device already booted errors
                        result.assertNormalExitValue()
                    }
                }
            }

            fun KotlinNativeTargetWithSimulatorTests.configureTestRun() {
                attributes.attribute(instrumentedTestAttribute, "test")
                testRuns.forEach {
                    (it as DefaultSimulatorTestRun).executionTask.configure { task ->
                        task.dependsOn(bootTask)
                        task.standalone.set(false)
                        task.device.set(getDeviceName())
                    }
                }
                compilations.forEach {
                    it.attributes.attribute(instrumentedTestCompilationAttribute, "test")
                }
            }

            iosX64("uikitInstrumentedX64") {
                configureTestRun()
            }
            // Testing on real iOS devices is not supported.
            // iosArm64("uikitInstrumentedArm64") { ... }
            iosSimulatorArm64("uikitInstrumentedSimArm64") {
                configureTestRun()
            }

            val uikitMain = sourceSets.getByName("uikitMain")
            val uikitInstrumentedMain = sourceSets.create("uikitInstrumentedMain")
            val uikitInstrumentedX64Main = sourceSets.getByName("uikitInstrumentedX64Main")
            val uikitInstrumentedSimArm64Main = sourceSets.getByName("uikitInstrumentedSimArm64Main")
            uikitInstrumentedMain.dependsOn(uikitMain)
            uikitInstrumentedX64Main.dependsOn(uikitInstrumentedMain)
            uikitInstrumentedSimArm64Main.dependsOn(uikitInstrumentedMain)

            val commonTest = sourceSets.getByName("commonTest")
            val uikitInstrumentedTest = sourceSets.create("uikitInstrumentedTest")
            val uikitInstrumentedX64Test = sourceSets.getByName("uikitInstrumentedX64Test")
            val uikitInstrumentedSimArm64Test = sourceSets.getByName("uikitInstrumentedSimArm64Test")
            uikitInstrumentedTest.dependsOn(commonTest)
            uikitInstrumentedX64Test.dependsOn(uikitInstrumentedTest)
            uikitInstrumentedSimArm64Test.dependsOn(uikitInstrumentedTest)
        }
}
