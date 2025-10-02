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
import org.gradle.api.tasks.Copy
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.getByName
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinJsCompilerType
import org.jetbrains.kotlin.gradle.plugin.KotlinSourceSet
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTargetWithSimulatorTests
import org.jetbrains.kotlin.konan.target.KonanTarget
import org.tomlj.Toml
import org.jetbrains.kotlin.gradle.targets.js.testing.KotlinJsTest

open class AndroidXComposeMultiplatformExtensionImpl @Inject constructor(
    val project: Project
) : AndroidXComposeMultiplatformExtension() {
    private val multiplatformExtension =
        project.extensions.getByType(KotlinMultiplatformExtension::class.java)

    private val skikoVersion: String

    fun KotlinJsTest.passTestFlagsToEnvironment() {
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

    val skikoWasm = project.configurations.findByName("skikoWasm")
        ?: project.configurations.create("skikoWasm")

    override fun js(): Unit = multiplatformExtension.run {
        js(KotlinJsCompilerType.IR) {
            browser {
                testTask {
                    it.passTestFlagsToEnvironment()

                    it.useKarma {
                        // We need to set up at least one browser here due to kotlin tooling limitations
                        // Actual browser configuration is set in mpp/karma.config.d/js/config.js
                        useChrome()
                        useFirefox()
                        useSafari()
                        useConfigDirectory(
                            project.rootProject.projectDir.resolve("mpp/karma.config.d/js")
                        )
                    }
                }
            }
        }

        val commonMain = sourceSets.getByName("commonMain")
        val jsMain = sourceSets.getByName("jsMain")
        jsMain.dependsOn(commonMain)

        val resourcesDir = project.buildDir.resolve("resources/skiko-js")

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

        sourceSets.getByName("jsTest").also {
            it.resources.setSrcDirs(it.resources.srcDirs)
            it.resources.srcDirs(fetchSkikoWasmRuntime.map { it.destinationDir })
        }
    }

    internal val Project.isInIdea: Boolean
        get() {
            return System.getProperty("idea.active")?.toBoolean() == true
        }

    @OptIn(org.jetbrains.kotlin.gradle.ExperimentalWasmDsl::class)
    override fun wasm(): Unit = multiplatformExtension.run {
        wasmJs {
            browser {
                testTask {
                    it.passTestFlagsToEnvironment()

                    it.useKarma {
                        // We need to set up at least one browser here due to kotlin tooling limitations
                        // Actual browser configuration is set in mpp/karma.config.d/wasm/config.js
                        useChrome()
                        useFirefox()
                        useSafari()
                        useConfigDirectory(
                            project.rootProject.projectDir.resolve("mpp/karma.config.d/wasm")
                        )
                    }
                }
            }
        }

        val resourcesDir = project.buildDir.resolve("resources/skiko-wasm")

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

        val commonMain = sourceSets.getByName("commonMain")
        val wasmMain = sourceSets.getByName("wasmJsMain")
        wasmMain.dependsOn(commonMain)

        sourceSets.getByName("wasmJsTest").also {
            it.resources.setSrcDirs(it.resources.srcDirs)
            it.resources.srcDirs(fetchSkikoWasmRuntime.map { it.destinationDir })
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

        val nativeMain = getOrCreateNativeMain()
        val darwinMain = sourceSets.create("darwinMain")
        val macosMain = sourceSets.create("macosMain")
        val macosX64Main = sourceSets.getByName("macosX64Main")
        val macosArm64Main = sourceSets.getByName("macosArm64Main")
        val uikitMain = sourceSets.create("uikitMain")
        val uikitX64Main = sourceSets.getByName("uikitX64Main")
        val uikitArm64Main = sourceSets.getByName("uikitArm64Main")
        val uikitSimArm64Main = sourceSets.getByName("uikitSimArm64Main")
        darwinMain.dependsOn(nativeMain)
        macosMain.dependsOn(darwinMain)
        macosX64Main.dependsOn(macosMain)
        macosArm64Main.dependsOn(macosMain)
        uikitMain.dependsOn(darwinMain)
        uikitX64Main.dependsOn(uikitMain)
        uikitArm64Main.dependsOn(uikitMain)
        uikitSimArm64Main.dependsOn(uikitMain)

        val nativeTest = getOrCreateNativeTest()
        val darwinTest = sourceSets.create("darwinTest")
        val macosTest = sourceSets.create("macosTest")
        val macosX64Test = sourceSets.getByName("macosX64Test")
        val macosArm64Test = sourceSets.getByName("macosArm64Test")
        val uikitTest = sourceSets.create("uikitTest")
        val uikitX64Test = sourceSets.getByName("uikitX64Test")
        val uikitArm64Test = sourceSets.getByName("uikitArm64Test")
        val uikitSimArm64Test = sourceSets.getByName("uikitSimArm64Test")
        darwinTest.dependsOn(nativeTest)
        macosTest.dependsOn(darwinTest)
        macosX64Test.dependsOn(macosTest)
        macosArm64Test.dependsOn(macosTest)
        uikitTest.dependsOn(darwinTest)
        uikitX64Test.dependsOn(uikitTest)
        uikitArm64Test.dependsOn(uikitTest)
        uikitSimArm64Test.dependsOn(uikitTest)
    }

    override fun linux(): Unit = multiplatformExtension.run {
        linuxX64()
        linuxArm64()

        val nativeMain = getOrCreateNativeMain()
        val linuxMain = sourceSets.create("linuxMain")
        val linuxX64Main = sourceSets.getByName("linuxX64Main")
        val linuxArm64Main = sourceSets.getByName("linuxArm64Main")
        linuxMain.dependsOn(nativeMain)
        linuxX64Main.dependsOn(linuxMain)
        linuxArm64Main.dependsOn(linuxMain)

        val nativeTest = getOrCreateNativeTest()
        val linuxTest = sourceSets.create("linuxTest")
        val linuxX64Test = sourceSets.getByName("linuxX64Test")
        val linuxArm64Test = sourceSets.getByName("linuxArm64Test")
        linuxTest.dependsOn(nativeTest)
        linuxX64Test.dependsOn(linuxTest)
        linuxArm64Test.dependsOn(linuxTest)
    }

    private fun getOrCreateJvmMain(): KotlinSourceSet =
        getOrCreateSourceSet("jvmMain", "commonMain")

    private fun getOrCreateJvmTest(): KotlinSourceSet =
        getOrCreateSourceSet("jvmTest", "commonTest")

    private fun getOrCreateNativeMain(): KotlinSourceSet =
        getOrCreateSourceSet("nativeMain", "commonMain")

    private fun getOrCreateNativeTest(): KotlinSourceSet =
        getOrCreateSourceSet("nativeTest", "commonTest")

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

                // TODO: Remove when the issue is fixed in KGP
                // https://youtrack.jetbrains.com/issue/KT-74564
                // it.freeCompilerArgs += flags
                //
                // Fixes problem when instrumented tests compilation is not properly applied to
                // the framework configuration.
                it.linkTaskProvider.configure {
                    @Suppress("DEPRECATION")
                    it.kotlinOptions.freeCompilerArgs += flags
                }
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

    override fun iosInstrumentedTest() {
        multiplatformExtension.run {
            val uikitInstrumentedTest = sourceSets.create("uikitInstrumentedTest")

            fun KotlinNativeTargetWithSimulatorTests.configureTestRun() {
                val testCompilation = compilations.create("instrumentedTest") {
                    compilerOptions {
                        // Generate K/N test runner for kotlin.test @Test support
                        freeCompilerArgs.add("-tr")
                    }

                    it.associateWith(compilations.getByName("test"))
                    it.defaultSourceSet.dependsOn(uikitInstrumentedTest)
                }
                binaries.framework("InstrumentedTest", setOf(DEBUG)) {
                    compilation = testCompilation
                    baseName = "InstrumentedTest"
                    isStatic = true
                }
            }
            testableTargets.getByName(
                "uikitX64",
                KotlinNativeTargetWithSimulatorTests::class,
                KotlinNativeTargetWithSimulatorTests::configureTestRun
            )
            testableTargets.getByName(
                "uikitSimArm64",
                KotlinNativeTargetWithSimulatorTests::class,
                KotlinNativeTargetWithSimulatorTests::configureTestRun
            )
        }
    }
}
