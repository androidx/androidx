/*
 * Copyright 2023 The Android Open Source Project
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

import androidx.build.AndroidXComposePlugin
import java.util.*
import org.jetbrains.androidx.build.JetBrainsAndroidXPlugin
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.NativeBuildType
import org.jetbrains.kotlin.gradle.targets.js.webpack.KotlinWebpackConfig

plugins {
    id("AndroidXPlugin")
    id("AndroidXComposePlugin")
    id("kotlin-multiplatform")
//  [1.4 Update]  id("application")
    kotlin("plugin.serialization") version "1.9.21"
    id("JetBrainsAndroidXPlugin")
}

AndroidXComposePlugin.applyAndConfigureKotlinPlugin(project)
JetBrainsAndroidXPlugin.applyAndConfigure(project)

dependencies {

}

val resourcesDir = "$buildDir/resources"
val skikoWasm = configurations.findByName("skikoWasm") ?: configurations.create("skikoWasm")

dependencies {
    skikoWasm(libs.skikoWasm)
}

val unzipTask = tasks.register("unzipWasm", Copy::class) {
    destinationDir = file(resourcesDir)
    from(skikoWasm.map { zipTree(it) })
}

repositories {
    mavenLocal()
}

kotlin {
    jvm("desktop")
    js {
        outputModuleName = "mpp-demo"
        browser {
            commonWebpackConfig {
                outputFileName = "demo.js"
            }
        }
        binaries.executable()
    }
    wasmJs {
        outputModuleName = "mpp-demo"
        browser {
            commonWebpackConfig {
                outputFileName = "demo.js"
                devServer = (devServer ?: KotlinWebpackConfig.DevServer()).apply {
                    open = mapOf(
                        "app" to mapOf(
                            "name" to "google-chrome",
                        )
                    )
                    static = (static ?: mutableListOf()).apply {
                        // Serve sources to debug inside browser
                        add(project.rootDir.path)
                        add(project.projectDir.path)
                    }
                }
            }
        }
        binaries.executable()
    }
    macosX64() {
        binaries {
            executable() {
                entryPoint = "androidx.compose.mpp.demo.main"
                freeCompilerArgs += listOf(
                    "-linker-option", "-framework", "-linker-option", "Metal"
                )
                // TODO: the current release binary surprises LLVM, so disable checks for now.
                freeCompilerArgs += "-Xdisable-phases=VerifyBitcode"
            }
        }
    }
    macosArm64() {
        binaries {
            executable() {
                entryPoint = "androidx.compose.mpp.demo.main"
                freeCompilerArgs += listOf(
                    "-linker-option", "-framework", "-linker-option", "Metal"
                )
                // TODO: the current release binary surprises LLVM, so disable checks for now.
                freeCompilerArgs += "-Xdisable-phases=VerifyBitcode"
            }
        }
    }
    iosX64("iosX64") {
        binaries {
            executable() {
                entryPoint = "androidx.compose.mpp.demo.main"
                freeCompilerArgs += listOf(
                    "-linker-option", "-framework", "-linker-option", "Metal",
                    "-linker-option", "-framework", "-linker-option", "CoreText",
                    "-linker-option", "-framework", "-linker-option", "CoreGraphics"
                )
                // TODO: the current compose binary surprises LLVM, so disable checks for now.
                freeCompilerArgs += "-Xdisable-phases=VerifyBitcode"
            }
        }
    }
    iosArm64("iosArm64") {
        binaries {
            executable() {
                entryPoint = "androidx.compose.mpp.demo.main"
                freeCompilerArgs += listOf(
                    "-linker-option", "-framework", "-linker-option", "Metal",
                    "-linker-option", "-framework", "-linker-option", "CoreText",
                    "-linker-option", "-framework", "-linker-option", "CoreGraphics"
                )
                // TODO: the current compose binary surprises LLVM, so disable checks for now.
                freeCompilerArgs += "-Xdisable-phases=VerifyBitcode"
            }
        }
    }
    iosSimulatorArm64("iosSimArm64") {
        binaries {
            executable() {
                entryPoint = "androidx.compose.mpp.demo.main"
                freeCompilerArgs += listOf(
                    "-linker-option", "-framework", "-linker-option", "Metal",
                    "-linker-option", "-framework", "-linker-option", "CoreText",
                    "-linker-option", "-framework", "-linker-option", "CoreGraphics"
                )
                // TODO: the current compose binary surprises LLVM, so disable checks for now.
                freeCompilerArgs += "-Xdisable-phases=VerifyBitcode"
            }
        }
    }
    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(project(":compose:foundation:foundation"))
                implementation(project(":compose:foundation:foundation-layout"))
                implementation(project(":compose:material3:material3"))
                implementation(project(":compose:material3:material3-window-size-class"))
                implementation(project(":compose:material3:adaptive:adaptive"))
                implementation(project(":compose:material3:adaptive:adaptive-layout"))
                implementation(project(":compose:material3:adaptive:adaptive-navigation"))
                implementation(project(":compose:material:material"))
                implementation(project(":compose:mpp"))
                implementation(project(":compose:runtime:runtime"))
                implementation(project(":compose:ui:ui"))
                implementation(project(":compose:ui:ui-graphics"))
                implementation(project(":compose:ui:ui-text"))
                implementation(project(":compose:ui:ui-backhandler"))
                implementation(project(":lifecycle:lifecycle-common"))
                implementation(project(":lifecycle:lifecycle-runtime"))
                implementation(project(":lifecycle:lifecycle-runtime-compose"))
                implementation(project(":lifecycle:lifecycle-viewmodel-compose"))
                implementation(project(":lifecycle:lifecycle-viewmodel-savedstate"))
                implementation(project(":navigation:navigation-common"))
                implementation(project(":navigation:navigation-compose"))
                implementation(project(":navigation:navigation-runtime"))
                implementation(libs.kotlinStdlib)
                implementation(libs.kotlinCoroutinesCore)
                api(libs.kotlinSerializationCore)

                implementation("org.jetbrains.compose.material:material-icons-core:1.7.3") {
                    // exclude dependencies, because they override local projects when we build 0.0.0-* version
                    // (see https://repo1.maven.org/maven2/org/jetbrains/compose/material/material-icons-core-desktop/1.6.11/material-icons-core-desktop-1.6.11.module)
                    exclude("org.jetbrains.compose.runtime")
                    exclude("org.jetbrains.compose.ui")
                }
            }
        }

        val skikoMain by creating {
            dependsOn(commonMain)
            dependencies {
                implementation(libs.skikoCommon)
            }
        }

        val desktopMain by getting {
            dependsOn(skikoMain)
            dependencies {
                implementation(libs.kotlinCoroutinesSwing)
                implementation(libs.skikoCurrentOs)
                implementation(project(":compose:desktop:desktop"))
            }
        }

        val webMain by creating {
            dependsOn(skikoMain)
            resources.setSrcDirs(resources.srcDirs)
            resources.srcDirs(unzipTask.map { it.destinationDir })

            dependencies {
                implementation(libs.kotlinSerializationJson)
            }
        }

        val jsMain by getting {
            dependsOn(webMain)
        }

        val wasmJsMain by getting {
            dependsOn(webMain)
            dependencies {
                api(libs.kotlinXw3c)
            }
        }

        val nativeMain by creating { dependsOn(skikoMain) }
        val darwinMain by creating { dependsOn(nativeMain) }
        val macosMain by creating { dependsOn(darwinMain) }
        val macosX64Main by getting { dependsOn(macosMain) }
        val macosArm64Main by getting { dependsOn(macosMain) }
        val iosMain by creating {
            kotlin.srcDir("src/uikitMain/kotlin")
            resources.srcDir("src/uikitMain/resources")
            dependsOn(darwinMain)
        }
        val iosX64Main by getting { dependsOn(iosMain) }
        val iosArm64Main by getting { dependsOn(iosMain) }
        val iosSimArm64Main by getting { dependsOn(iosMain) }
    }
}

enum class Target(val simulator: Boolean, val key: String) {
    IOS_X64(true, "iosX64"),
    IOS_ARM64(false, "iosArm64"),
    IOS_SIM_ARM64(true, "iosSimArm64"),
}

if (System.getProperty("os.name") == "Mac OS X") {
// Create Xcode integration tasks.
    val sdkName: String? = System.getenv("SDK_NAME")

    val target = sdkName.orEmpty().let {
        when {
            it.startsWith("iphoneos") -> Target.IOS_ARM64
            it.startsWith("iphonesimulator") -> {
                if (System.getProperty("os.arch") == "aarch64") {
                    Target.IOS_SIM_ARM64
                } else {
                    Target.IOS_X64
                }
            }

            else -> Target.IOS_X64
        }
    }

    val targetBuildDir: String? = System.getenv("TARGET_BUILD_DIR")
    val executablePath: String? = System.getenv("EXECUTABLE_PATH")
    val buildType = System.getenv("CONFIGURATION")
        ?.let { NativeBuildType.valueOf(it.uppercase(Locale.getDefault())) }
        ?: NativeBuildType.DEBUG

    val currentTarget = kotlin.targets[target.key] as KotlinNativeTarget
    val kotlinBinary = currentTarget.binaries.getExecutable(buildType)
    val xcodeIntegrationGroup = "Xcode integration"

    val packForXCode = if (sdkName == null || targetBuildDir == null || executablePath == null) {
        // The build is launched not by Xcode ->
        // We cannot create a copy task and just show a meaningful error message.
        tasks.create("packForXCode").doLast {
            group = xcodeIntegrationGroup
            throw IllegalStateException("Please run the task from Xcode")
        }
    } else {
        // Otherwise copy the executable into the Xcode output directory.
        tasks.create("packForXCode", Copy::class.java) {
            dependsOn(kotlinBinary.linkTaskProvider)

            group = xcodeIntegrationGroup
            destinationDir = file(targetBuildDir)

            val dsymSource = kotlinBinary.outputFile.absolutePath + ".dSYM"
            val dsymDestination = File(executablePath).parentFile.name + ".dSYM"
            val oldExecName = kotlinBinary.outputFile.name
            val newExecName = File(executablePath).name

            from(dsymSource) {
                into(dsymDestination)
                rename(oldExecName, newExecName)
            }

            from(kotlinBinary.outputFile) {
                rename { executablePath }
            }
        }
    }
}

tasks.create("runDesktop", JavaExec::class.java) {
    dependsOn(":compose:desktop:desktop:jvmJar")
    mainClass.set("androidx.compose.mpp.demo.Main_desktopKt")
    args = listOfNotNull(project.findProperty("args")?.toString())
    systemProperty("skiko.fps.enabled", "true")
    val compilation = kotlin.jvm("desktop").compilations["main"]
    classpath =
        compilation.output.allOutputs +
            compilation.runtimeDependencyFiles
}


project.tasks.withType<org.jetbrains.kotlin.gradle.dsl.KotlinJsCompile>().configureEach {
    compilerOptions.freeCompilerArgs.addAll(
        "-Xir-dce",
        "-Xwasm-generate-wat",
        "-Xwasm-enable-array-range-checks"
    )
}
