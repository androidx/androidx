/*
 * Copyright 2022 The Android Open Source Project
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

import groovy.lang.Closure
import org.gradle.api.Action
import org.gradle.api.NamedDomainObjectCollection
import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.dsl.KotlinMultiplatformExtension
import org.jetbrains.kotlin.gradle.plugin.KotlinMultiplatformPluginWrapper
import org.jetbrains.kotlin.gradle.plugin.KotlinPlatformType
import org.jetbrains.kotlin.gradle.plugin.KotlinTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinAndroidTarget
import org.jetbrains.kotlin.gradle.plugin.KotlinTargetPreset
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTargetWithHostTests
import org.jetbrains.kotlin.gradle.targets.js.dsl.KotlinJsTargetDsl
import org.jetbrains.kotlin.gradle.targets.jvm.KotlinJvmTarget

/**
 *  [AndroidXMultiplatformExtension] is an extension that wraps specific functionality of the Kotlin
 *  multiplatform extension, and applies the Kotlin multiplatform plugin when it is used. The
 *  purpose of wrapping is to prevent targets from being added when the platform has not been
 *  enabled. e.g. the `macosX64` target is gated on a `project.enableMac` check.
 */
open class AndroidXMultiplatformExtension(val project: Project) {

    // Kotlin multiplatform plugin is only applied if at least one target / sourceset is added.
    private val kotlinExtensionDelegate = lazy {
        project.validateMultiplatformPluginHasNotBeenApplied()
        project.plugins.apply(KotlinMultiplatformPluginWrapper::class.java)
        project.multiplatformExtension!!
    }
    private val kotlinExtension: KotlinMultiplatformExtension by kotlinExtensionDelegate

    val presets: NamedDomainObjectCollection<KotlinTargetPreset<*>>
        get() = kotlinExtension.presets
    val targets: NamedDomainObjectCollection<KotlinTarget>
        get() = kotlinExtension.targets

    internal fun hasNativeTarget(): Boolean {
        // it is important to check initialized here not to trigger initialization
        return kotlinExtensionDelegate.isInitialized() && targets.any {
            it.platformType == KotlinPlatformType.native
        }
    }
    fun sourceSets(closure: Closure<*>) {
        if (kotlinExtensionDelegate.isInitialized()) {
            kotlinExtension.sourceSets.configure(closure)
        }
    }

    @JvmOverloads
    fun jvm(
        block: Action<KotlinJvmTarget>? = null
    ): KotlinJvmTarget? {
        return if (project.enableJvm()) {
            kotlinExtension.jvm {
                block?.execute(this)
            }
        } else { null }
    }

    @JvmOverloads
    fun android(
        block: Action<KotlinAndroidTarget>? = null
    ): KotlinAndroidTarget? {
        return if (project.enableJvm()) {
            kotlinExtension.androidTarget {
                block?.execute(this)
            }
        } else { null }
    }

    @JvmOverloads
    fun desktop(
        block: Action<KotlinJvmTarget>? = null
    ): KotlinJvmTarget? {
        return if (project.enableJvm()) {
            kotlinExtension.jvm("desktop") {
                block?.execute(this)
            }
        } else { null }
    }

    /**
     * Configures all mac targets supported by AndroidX.
     */
    @JvmOverloads
    fun mac(
        block: Action<KotlinNativeTarget>? = null
    ): List<KotlinNativeTarget> {
        return listOfNotNull(
            macosX64(block),
            macosArm64(block)
        )
    }

    @JvmOverloads
    fun macosX64(
        block: Action<KotlinNativeTarget>? = null
    ): KotlinNativeTargetWithHostTests? {
        return if (project.enableMac()) {
            kotlinExtension.macosX64().also {
                block?.execute(it)
            }
        } else { null }
    }

    @JvmOverloads
    fun macosArm64(
        block: Action<KotlinNativeTarget>? = null
    ): KotlinNativeTargetWithHostTests? {
        return if (project.enableMac()) {
            kotlinExtension.macosArm64().also {
                block?.execute(it)
            }
        } else { null }
    }

    @JvmOverloads
    fun iosArm64(
        block: Action<KotlinNativeTarget>? = null
    ): KotlinNativeTarget? {
        return if (project.enableMac()) {
            kotlinExtension.iosArm64().also {
                block?.execute(it)
            }
        } else { null }
    }

    /**
     * Configures all ios targets supported by AndroidX.
     */
    @JvmOverloads
    fun ios(
        block: Action<KotlinNativeTarget>? = null
    ): List<KotlinNativeTarget> {
        return listOfNotNull(
            iosX64(block),
            iosArm64(block),
            iosSimulatorArm64(block)
        )
    }
    @JvmOverloads
    fun iosX64(
        block: Action<KotlinNativeTarget>? = null
    ): KotlinNativeTarget? {
        return if (project.enableMac()) {
            kotlinExtension.iosX64().also {
                block?.execute(it)
            }
        } else { null }
    }

    @JvmOverloads
    fun iosSimulatorArm64(
        block: Action<KotlinNativeTarget>? = null
    ): KotlinNativeTarget? {
        return if (project.enableMac()) {
            kotlinExtension.iosSimulatorArm64().also {
                block?.execute(it)
            }
        } else { null }
    }

    @JvmOverloads
    fun linux(
        block: Action<KotlinNativeTarget>? = null
    ): List<KotlinNativeTarget> {
        return listOfNotNull(
            linuxX64(block),
        )
    }

    @JvmOverloads
    fun linuxX64(
        block: Action<KotlinNativeTarget>? = null
    ): KotlinNativeTargetWithHostTests? {
        return if (project.enableLinux()) {
            kotlinExtension.linuxX64().also {
                block?.execute(it)
            }
        } else { null }
    }

    @JvmOverloads
    fun js(
        block: Action<KotlinJsTargetDsl>? = null
    ): KotlinJsTargetDsl? {
        return if (project.enableJs()) {
            kotlinExtension.js().also {
                block?.execute(it)
            }
        } else {
            null
        }
    }

    companion object {
        const val EXTENSION_NAME = "androidXMultiplatform"
    }
}

/**
 * Returns a provider that is set to true if and only if this project has at least 1 kotlin native
 * target (mac, linux, ios).
 */
internal fun Project.hasKotlinNativeTarget() = project.provider {
    project.extensions.getByType(AndroidXMultiplatformExtension::class.java).hasNativeTarget()
}
