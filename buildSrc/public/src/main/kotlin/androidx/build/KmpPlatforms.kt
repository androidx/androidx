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
    MAC,
    LINUX,
    DESKTOP;

    companion object {
        /** Target platform groups which require native compilation (e.g. LLVM). */
        val native = listOf(MAC, LINUX)

        /**
         * Target platform groups which are enabled by default.
         *
         * Do *not* enable [JS] unless you have read and understand this:
         * https://blog.jetbrains.com/kotlin/2021/10/important-ua-parser-js-exploit-and-kotlin-js/
         */
        val enabledByDefault = listOf(JVM, DESKTOP)
    }
}

/** Target platforms supported by the AndroidX implementation of Kotlin multi-platform. */
enum class PlatformIdentifier(
    val id: String,
    @Suppress("unused") private val group: PlatformGroup
) {
    JVM("jvm", PlatformGroup.JVM),
    JS("js", PlatformGroup.JS),
    ANDROID("android", PlatformGroup.JVM),
    MAC_ARM_64("macosarm64", PlatformGroup.MAC),
    MAC_OSX_64("macosx64", PlatformGroup.MAC),
    LINUX_64("linuxx64", PlatformGroup.LINUX),
    IOS_SIMULATOR_ARM_64("iossimulatorarm64", PlatformGroup.MAC),
    IOS_X_64("iosx64", PlatformGroup.MAC),
    IOS_ARM_64("iosarm64", PlatformGroup.MAC),
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

/** Returns true if kotlin native targets should be enabled. */
private fun Project.isKotlinNativeEnabled(): Boolean {
    return "KMP".equals(System.getenv()["ANDROIDX_PROJECTS"], ignoreCase = true) ||
        "INFRAROGUE".equals(System.getenv()["ANDROIDX_PROJECTS"], ignoreCase = true) ||
        ProjectLayoutType.isPlayground(project) ||
        project.providers.gradleProperty("androidx.kmp.native.enabled").orNull?.toBoolean() == true
}

/** Extension used to store parsed KMP configuration information. */
private open class KmpPlatformsExtension(project: Project) {
    val enabledKmpPlatforms =
        parseTargetPlatformsFlag(project.findProperty(ENABLED_KMP_TARGET_PLATFORMS) as? String)
}

fun Project.enableJs(): Boolean = enabledKmpPlatforms.contains(PlatformGroup.JS)

fun Project.enableMac(): Boolean =
    enabledKmpPlatforms.contains(PlatformGroup.MAC) || isKotlinNativeEnabled()

fun Project.enableLinux(): Boolean =
    enabledKmpPlatforms.contains(PlatformGroup.LINUX) || isKotlinNativeEnabled()

fun Project.enableJvm(): Boolean = enabledKmpPlatforms.contains(PlatformGroup.JVM)

fun Project.enableDesktop(): Boolean = enabledKmpPlatforms.contains(PlatformGroup.DESKTOP)

fun Project.enableNative(): Boolean = enableMac() && enableLinux()
