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

package org.jetbrains.androidx.build

import androidx.build.AndroidXMultiplatformExtension
import androidx.build.PlatformIdentifier
import androidx.build.configurePinnedKotlinLibraries
import androidx.build.multiplatformExtension
import org.gradle.api.Action
import org.gradle.api.Project
import org.gradle.api.tasks.Copy
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.getByName
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTargetWithSimulatorTests
import org.jetbrains.kotlin.gradle.targets.js.dsl.KotlinJsTargetDsl
import org.jetbrains.kotlin.gradle.targets.js.testing.KotlinJsTest
import org.jetbrains.kotlin.konan.target.KonanTarget
import org.tomlj.Toml

private fun KotlinJsTest.passTestFlagsToEnvironment() {
    listOf(
        "jetbrains.androidx.web.tests.enableChrome",
        "jetbrains.androidx.web.tests.enableChromium",
        "jetbrains.androidx.web.tests.enableFirefox",
        "jetbrains.androidx.web.tests.enableSafari",
    ).forEach { propertyName ->
        if (project.findProperty(propertyName)?.toString()?.toBoolean() == true) {
            environment(propertyName, "1")
        }
    }
}

fun <T> AndroidXMultiplatformExtension.configureForkWebTarget(
    platform: PlatformIdentifier,
    isEnabled: Boolean,
    createTarget: (KotlinJsTargetDsl.() -> Unit) -> T,
    block: Action<KotlinJsTargetDsl>? = null,
): T? {
    val toml = Toml.parse(
        project.rootProject.projectDir.resolve("gradle/libs.versions.toml").toPath()
    )
    val skikoVersion = toml.getTable("versions")!!.getString("skiko")!!
    val skikoWasm = project.configurations.findByName("skikoWasm")
        ?: project.configurations.create("skikoWasm")

    supportedPlatforms.add(platform)
    return if (isEnabled) {
        val target = createTarget {
            block?.execute(this)
            project.configurePinnedKotlinLibraries(platform)
            browser {
                testTask {
                    it.testLogging.showStandardStreams = true
                    it.testLogging.showExceptions = true
                    // We need to set up at least one browser here due to kotlin tooling limitations
                    // Actual browser configuration is set in mpp/karma.config.d/js/config.js
                    it.passTestFlagsToEnvironment()
                    it.useKarma {
                        useChrome()
                        useFirefox()
                        useSafari()
                        useConfigDirectory(
                            project.rootProject.projectDir.resolve(
                                if (platform == PlatformIdentifier.JS) {
                                    "mpp/karma.config.d/js"
                                } else {
                                    "mpp/karma.config.d/wasm"
                                }
                            )
                        )
                    }
                }
            }
        }

        if (platform == PlatformIdentifier.JS) {
            val resourcesDir = project.layout.buildDirectory.asFile.get().resolve("resources/skiko-js")

            // Below code helps configure the tests for k/wasm targets
            project.dependencies {
                skikoWasm("org.jetbrains.skiko:skiko-js-wasm-runtime:${skikoVersion}")
            }

            val fetchSkikoWasmRuntime = project.tasks.register("fetchSkikoJsWasmRuntime", Copy::class.java) {
                it.destinationDir = project.file(resourcesDir)
                it.from(skikoWasm.map { artifact ->
                    project.zipTree(artifact)
                        .matching { pattern ->
                            pattern.include("skiko.wasm", "skiko.mjs", "js-reexport-symbols.mjs")
                        }
                })
            }

            project.tasks.getByName("jsTestProcessResources").apply {
                dependsOn(fetchSkikoWasmRuntime)
            }

            project.multiplatformExtension!!.sourceSets.getByName("jsTest").also {
                it.resources.setSrcDirs(it.resources.srcDirs)
                it.resources.srcDirs(fetchSkikoWasmRuntime.map { it.destinationDir })
            }
        } else {
            val resourcesDir = project.layout.buildDirectory.asFile.get().resolve("resources/skiko-wasm")

            // Below code helps configure the tests for k/wasm targets
            project.dependencies {
                skikoWasm("org.jetbrains.skiko:skiko-js-wasm-runtime:${skikoVersion}")
            }

            val fetchSkikoWasmRuntime = project.tasks.register("fetchSkikoWasmRuntime", Copy::class.java) {
                it.destinationDir = project.file(resourcesDir)
                it.from(skikoWasm.map { artifact ->
                    project.zipTree(artifact)
                        .matching { pattern ->
                            pattern.include("skiko.wasm", "skiko.mjs")
                        }
                })
            }

            project.tasks.getByName("wasmJsTestProcessResources").apply {
                dependsOn(fetchSkikoWasmRuntime)
            }

            project.multiplatformExtension!!.sourceSets.getByName("wasmJsTest").also {
                it.resources.setSrcDirs(it.resources.srcDirs)
                it.resources.srcDirs(fetchSkikoWasmRuntime.map { it.destinationDir })
            }
        }

        target
    } else null
}

/**
 * Configures native compilation tasks with flags to link required frameworks
 */
fun configureDarwinFlags(project: Project) {
    val darwinFlags = listOf(
        "-linker-option", "-framework", "-linker-option", "Metal",
        "-linker-option", "-framework", "-linker-option", "CoreText",
        "-linker-option", "-framework", "-linker-option", "CoreGraphics",
        "-linker-option", "-framework", "-linker-option", "CoreServices"
    )
    val iosFlags = listOf("-linker-option", "-framework", "-linker-option", "UIKit")

    fun KotlinNativeTarget.configureFreeCompilerArgs() {
        val isIOS = konanTarget == KonanTarget.IOS_SIMULATOR_ARM64 ||
            konanTarget == KonanTarget.IOS_ARM64

        binaries.forEach {
            val flags = mutableListOf<String>().apply {
                addAll(darwinFlags)
                if (isIOS) addAll(iosFlags)
            }

             it.freeCompilerArgs += flags
        }
    }
    project.multiplatformExtension!!.run {
        macosArm64 { configureFreeCompilerArgs() }
        iosArm64 { configureFreeCompilerArgs() }
        iosSimulatorArm64 { configureFreeCompilerArgs() }
    }
}

/**
 * Configure instrumented tests to run on an actual iOS simulator.
 */
fun addIosInstrumentedTestSourceset(project: Project) {
    project.multiplatformExtension!!.run {
        val iosInstrumentedTest = sourceSets.create("iosInstrumentedTest")
        iosInstrumentedTest.kotlin.srcDir("src/uikitInstrumentedTest/kotlin")

        fun KotlinNativeTargetWithSimulatorTests.configureTestRun() {
            val testCompilation = compilations.create("instrumentedTest") {
                compilerOptions {
                    // Generate K/N test runner for kotlin.test @Test support
                    freeCompilerArgs.add("-tr")
                }

                it.associateWith(compilations.getByName("test"))
                it.defaultSourceSet.dependsOn(iosInstrumentedTest)
            }
            binaries.framework("InstrumentedTest", setOf(DEBUG)) {
                compilation = testCompilation
                baseName = "InstrumentedTest"
                isStatic = true
            }
        }
        testableTargets.getByName(
            "iosSimulatorArm64",
            KotlinNativeTargetWithSimulatorTests::class,
            KotlinNativeTargetWithSimulatorTests::configureTestRun
        )
    }
}
