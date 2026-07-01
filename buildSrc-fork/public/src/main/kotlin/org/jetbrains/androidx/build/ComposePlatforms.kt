/*
 * Copyright 2020-2021 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */
package org.jetbrains.androidx.build

import java.util.*
import org.gradle.api.Project

/**
 * The name or alternative names can be used in gradle.properties of the modules (in arbitrary case).
 * That means we need to be careful if/when renaming or deleting any enum value or its name.
 */
enum class ComposePlatforms(vararg val alternativeNames: String) {
    KotlinMultiplatform("Common", "Metadata"),
    Desktop("Jvm"),
    Android("Android"),
    Js("Web"),
    WasmJs("Web"),
    MacosArm64("Macos"),
    IosArm64("Ios"),
    IosSimulatorArm64("Ios"),
    TvosArm64("TvOs"),
    TvosSimulatorArm64("TvOs"),
    WatchosArm64("WatchOs"),
    WatchosArm32("WatchOs"),
    WatchosSimulatorArm64("WatchOs"),
    LinuxX64("Linux"),
    LinuxArm64("Linux"),
    MingwX64("Mingw"),
    ;

    private val namesLowerCased by lazy {
        listOf(name, *alternativeNames).map { it.lowercase() }.toSet()
    }

    fun matchesAnyIgnoringCase(namesToMatch: Collection<String>): Boolean {
        val namesToMatchLowerCased = namesToMatch.map { it.lowercase() }.toSet()
        return namesToMatchLowerCased.intersect(this.namesLowerCased).isNotEmpty()
    }

    fun matches(nameCandidate: String): Boolean =
        listOf(name, *alternativeNames).any { it.equals(nameCandidate, ignoreCase = true) }

    companion object {
        val JVM_BASED = EnumSet.of(
            Desktop,
            Android
        )

        val IOS = EnumSet.of(
            IosArm64,
            IosSimulatorArm64
        )

        val TV_OS = EnumSet.of(
            TvosArm64,
            TvosSimulatorArm64
        )

        val WATCH_OS = EnumSet.of(
            WatchosArm64,
            WatchosArm32,
            WatchosSimulatorArm64
        )

        val ANDROID = EnumSet.of(
            Android
        )

        val WINDOWS_NATIVE = EnumSet.of(
            MingwX64
        )

        val LINUX_NATIVE = EnumSet.of(
            LinuxX64,
            LinuxArm64
        )

        val MACOS_NATIVE = EnumSet.of(
            MacosArm64
        )

        val WEB = EnumSet.of(
            Js,
            WasmJs
        )

        val DARWIN = IOS + WATCH_OS + TV_OS + MACOS_NATIVE

        val GENERATE_KLIB = WEB + LINUX_NATIVE + WINDOWS_NATIVE + DARWIN

        val SKIKO_SUPPORT =
            EnumSet.of(KotlinMultiplatform) + JVM_BASED + IOS + MACOS_NATIVE + WEB

        val ALL = EnumSet.allOf(ComposePlatforms::class.java)

        /**
         * Maps comma separated list of platforms into a set of [ComposePlatforms]
         * The function is case- and whitespace-insensetive.
         *
         * Special value: all
         */
        fun parse(platformsNames: String): Set<ComposePlatforms> {
            val platforms = EnumSet.noneOf(ComposePlatforms::class.java)
            val unknownNames = arrayListOf<String>()

            for (name in platformsNames.split(",").map { it.trim() }) {
                if (name.equals("all", ignoreCase = true)) {
                    return ALL
                }

                val matchingPlatforms = ALL.filter { it.matches(name) }
                if (matchingPlatforms.isNotEmpty()) {
                    platforms.addAll(matchingPlatforms)
                } else {
                    unknownNames.add(name)
                }
            }

            if (unknownNames.isNotEmpty()) {
                error("Unknown platforms: ${unknownNames.joinToString(", ")}")
            }

            return platforms
        }
    }
}
