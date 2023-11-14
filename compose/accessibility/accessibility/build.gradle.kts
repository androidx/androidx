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

import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.jetbrains.kotlin.konan.target.KonanTarget

plugins {
    id("kotlin-multiplatform")
    id("maven-publish")
}

version = "1.0-SNAPSHOT"
group = "me.user.accessibility"

// Kotlin library: accessibility:accessibility
// Package: androidx.compose.ui.uikit.accessibility
// Objc-Library: CMPAccessibility

kotlin {
    iosX64("uikitX64") {
        configureCInterop()
    }
    iosArm64("uikitArm64") {
        configureCInterop()
    }
    iosSimulatorArm64("uikitSimArm64") {
        configureCInterop()
    }

    sourceSets {
        val commonMain by getting
        val uikitMain = sourceSets.create("uikitMain")
        val uikitX64Main = sourceSets.getByName("uikitX64Main")
        val uikitArm64Main = sourceSets.getByName("uikitArm64Main")
        val uikitSimArm64Main = sourceSets.getByName("uikitSimArm64Main")

        uikitMain.dependsOn(commonMain)
        uikitX64Main.dependsOn(uikitMain)
        uikitArm64Main.dependsOn(uikitMain)
        uikitSimArm64Main.dependsOn(uikitMain)
    }
}

fun KotlinNativeTarget.configureCInterop() {
    val isDevice = konanTarget == KonanTarget.IOS_ARM64
    val frameworkName = "CMPAccessibility"
    val schemeName = frameworkName
    val objcDir = File(project.projectDir, "src/uikitMain/objc")
    val frameworkSourcesDir = File(objcDir, frameworkName)
    val platform = if (isDevice) "device" else "simulator"
    val buildDir = File(project.buildDir, "objc/$platform.xcarchive")
    val frameworkPath = File(buildDir, "/Products/usr/local/lib/lib$frameworkName.a")
    val headersPath = File(frameworkSourcesDir, frameworkName)

    val systemFrameworks = listOf("UIKit", frameworkName)
    val linkerFlags = listOf("-ObjC") + systemFrameworks.flatMap {
        listOf("-framework", it)
    }
    val compilerArgs = listOf(
        "-include-binary", frameworkPath.toString(),
    ) + linkerFlags.flatMap {
        listOf("-linker-option", it)
    }

    compilations.getByName("main") {
        cinterops.create("accessibility") {
            val taskName = "${interopProcessingTaskName}MakeFramework"
            project.tasks.register(taskName, Exec::class.java) {
                inputs.dir(frameworkSourcesDir)
                    .withPropertyName("$frameworkName-$platform")
                    .withPathSensitivity(PathSensitivity.RELATIVE)

                outputs.cacheIf { true }
                outputs.dir(buildDir)
                    .withPropertyName("$frameworkName-$platform-archive")

                workingDir(frameworkSourcesDir)
                commandLine("xcodebuild")
                args(
                    "archive",
                    "-scheme", schemeName,
                    "-archivePath", buildDir,
                    "-sdk", if (isDevice) "iphoneos" else "iphonesimulator",
                    "-destination", if (isDevice) {
                        "generic/platform=iOS"
                    } else {
                        "generic/platform=iOS Simulator"
                    },
                    "SKIP_INSTALL=NO",
                    "BUILD_LIBRARY_FOR_DISTRIBUTION=YES",
                    "VALID_ARCHS=" + if (isDevice) "arm64" else "arm64 x86_64",
                    "MACH_O_TYPE=staticlib"
                )
            }

            project.tasks.findByName(interopProcessingTaskName)!!
                .dependsOn(taskName)

            headersPath.walk().filter { it.isFile && it.extension == "h" }.forEach {
                headers(it)
            }
        }
    }

    binaries.all {
        freeCompilerArgs += compilerArgs
    }
    compilations.all {
        kotlinOptions {
            freeCompilerArgs += compilerArgs
        }
    }
}