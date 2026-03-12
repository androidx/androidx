/*
 * Copyright 2026 The Android Open Source Project
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

import org.jetbrains.androidx.build.JETBRAINS_COMPILE_KOTLIN_VERSION
import org.jetbrains.kotlin.gradle.dsl.KotlinNativeBinaryContainer
import org.jetbrains.kotlin.gradle.dsl.*

plugins {
    id("AndroidXPlugin")
    id("AndroidXComposePlugin")
    id("kotlin-multiplatform")
    id("JetBrainsAndroidXPlugin")
}

fun KotlinNativeBinaryContainer.configureFramework() {
    framework {
        baseName = "shared"
        freeCompilerArgs += listOf(
            "-linker-option", "-framework", "-linker-option", "Metal",
            "-linker-option", "-framework", "-linker-option", "CoreText",
            "-linker-option", "-framework", "-linker-option", "CoreGraphics"
        )
    }
}

kotlin {
    compilerOptions {
        languageVersion.set(JETBRAINS_COMPILE_KOTLIN_VERSION.apiVersion)
        apiVersion.set(JETBRAINS_COMPILE_KOTLIN_VERSION.apiVersion)
    }
    iosArm64 {
        binaries {
            configureFramework()
        }
    }
    iosSimulatorArm64 {
        binaries {
            configureFramework()
        }
    }
    sourceSets {
        val commonMain by getting {
            dependencies {
                implementation(project(":compose:foundation:foundation"))
                implementation(project(":compose:foundation:foundation-layout"))
                implementation(project(":compose:material:material"))
                implementation(project(":compose:mpp:demo"))
                implementation(project(":compose:runtime:runtime"))
                implementation(project(":compose:ui:ui"))
                implementation(project(":compose:ui:ui-graphics"))
                implementation(project(":compose:ui:ui-text"))
                implementation(libs.kotlinCoroutinesCore)
            }
        }
    }
}
