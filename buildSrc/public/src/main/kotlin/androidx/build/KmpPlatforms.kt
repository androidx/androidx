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

import androidx.build.gradle.extraPropertyOrNull
import java.util.Locale
import org.gradle.api.Project
import org.gradle.kotlin.dsl.create
import org.gradle.kotlin.dsl.findByType

/**
 * A comma-separated list of target platform groups you wish to enable or disable.
 *
 * For example, `-jvm,+mac,+linux,+js` disables all JVM (including Android) target platforms and
 * enables all Mac (including iOS), Linux, and JavaScript target platforms.
 */
const val ENABLED_KMP_TARGET_PLATFORMS = "androidx.enabled.kmp.target.platforms"

/** Target platform groups supported by the AndroidX implementation of Kotlin multi-platform. */
enum class PlatformGroup {
    JVM,
    JS,
    WASM,
    MAC,
    WINDOWS,
    LINUX,
    DESKTOP,
    ANDROID_NATIVE;

    companion object {
        /** Target platform groups which require native compilation (e.g. LLVM). */
        val native = listOf(MAC, LINUX, WINDOWS, ANDROID_NATIVE)

        /**
         * Target platform groups which are enabled by default. We currently enable all platforms by
         * default.
         */
        val enabledByDefault = listOf(ANDROID_NATIVE, DESKTOP, JS, JVM, LINUX, MAC, WASM, WINDOWS)
    }
}

/** Target platforms supported by the AndroidX implementation of Kotlin multi-platform. */
enum class PlatformIdentifier(val id: String, val group: PlatformGroup) {
    JVM("jvm", PlatformGroup.JVM),
    JVM_STUBS("jvmStubs", PlatformGroup.JVM),
    JS("js", PlatformGroup.JS),
    WASM_JS("wasmJs", PlatformGroup.WASM),
    ANDROID("android", PlatformGroup.JVM),
    ANDROID_NATIVE_ARM32("androidNativeArm32", PlatformGroup.ANDROID_NATIVE),
    ANDROID_NATIVE_ARM64("androidNativeArm64", PlatformGroup.ANDROID_NATIVE),
    ANDROID_NATIVE_X86("androidNativeX86", PlatformGroup.ANDROID_NATIVE),
    ANDROID_NATIVE_X64("androidNativeX64", PlatformGroup.ANDROID_NATIVE),
    MAC_ARM_64("macosarm64", PlatformGroup.MAC),
    MAC_OSX_64("macosx64", PlatformGroup.MAC),
    MINGW_X_64("mingwx64", PlatformGroup.WINDOWS),
    LINUX_ARM_64("linuxarm64", PlatformGroup.LINUX),
    LINUX_X_64("linuxx64", PlatformGroup.LINUX),
    LINUX_X_64_STUBS("linuxx64Stubs", PlatformGroup.LINUX),
    IOS_SIMULATOR_ARM_64("iossimulatorarm64", PlatformGroup.MAC),
    IOS_X_64("iosx64", PlatformGroup.MAC),
    IOS_ARM_64("iosarm64", PlatformGroup.MAC),
    WATCHOS_SIMULATOR_ARM_64("watchossimulatorarm64", PlatformGroup.MAC),
    WATCHOS_X_64("watchosx64", PlatformGroup.MAC),
    WATCHOS_ARM_32("watchosarm64", PlatformGroup.MAC),
    WATCHOS_ARM_64("watchosarm64", PlatformGroup.MAC),
    WATCHOS_DEVICE_ARM_64("watchosdevicearm64", PlatformGroup.MAC),
    TVOS_SIMULATOR_ARM_64("tvossimulatorarm64", PlatformGroup.MAC),
    TVOS_X_64("tvosx64", PlatformGroup.MAC),
    TVOS_ARM_64("tvosarm64", PlatformGroup.MAC),
    DESKTOP("desktop", PlatformGroup.JVM);

    companion object {
        private val byId = values().associateBy { it.id }

        fun fromId(id: String): PlatformIdentifier? = byId[id]
    }
}

fun parseTargetPlatformsFlag(flag: String?): Set<PlatformGroup> {
    if (flag.isNullOrBlank()) {
        return PlatformGroup.enabledByDefault.toSortedSet()
    }
    val enabled = PlatformGroup.enabledByDefault.toMutableList()
    flag.split(",").forEach {
        val directive = it.firstOrNull() ?: ""
        val platform = it.drop(1)
        when (directive) {
            '+' -> enabled.addAll(matchingPlatformGroups(platform))
            '-' -> enabled.removeAll(matchingPlatformGroups(platform))
            else -> {
                throw RuntimeException("Invalid value $flag for $ENABLED_KMP_TARGET_PLATFORMS")
            }
        }
    }
    return enabled.toSortedSet()
}

private fun matchingPlatformGroups(flag: String) =
    if (flag == "native") {
        PlatformGroup.native
    } else {
        listOf(PlatformGroup.valueOf(flag.uppercase(Locale.getDefault())))
    }

private val Project.enabledKmpPlatforms: Set<PlatformGroup>
    get() {
        val extension: KmpPlatformsExtension =
            extensions.findByType() ?: extensions.create("androidx.build.KmpPlatforms", this)
        return extension.enabledKmpPlatforms
    }

/** Extension used to store parsed KMP configuration information. */
private open class KmpPlatformsExtension(project: Project) {
    val enabledKmpPlatforms =
        parseTargetPlatformsFlag(
            project.extraPropertyOrNull(ENABLED_KMP_TARGET_PLATFORMS) as? String
        )
}

fun Project.enableJs(): Boolean = enabledKmpPlatforms.contains(PlatformGroup.JS)

fun Project.enableAndroidNative(): Boolean =
    enabledKmpPlatforms.contains(PlatformGroup.ANDROID_NATIVE)

fun Project.enableMac(): Boolean = enabledKmpPlatforms.contains(PlatformGroup.MAC)

fun Project.enableWindows(): Boolean = enabledKmpPlatforms.contains(PlatformGroup.WINDOWS)

fun Project.enableLinux(): Boolean = enabledKmpPlatforms.contains(PlatformGroup.LINUX)

fun Project.enableJvm(): Boolean = enabledKmpPlatforms.contains(PlatformGroup.JVM)

fun Project.enableDesktop(): Boolean = enabledKmpPlatforms.contains(PlatformGroup.DESKTOP)

fun Project.enableWasmJs(): Boolean = enabledKmpPlatforms.contains(PlatformGroup.WASM)
