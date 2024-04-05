/*
 * Copyright 2020-2021 JetBrains s.r.o. and respective authors and developers.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the LICENSE.txt file.
 */

import java.util.*

/**
 * The name or alternative names can be used in gradle.properties of the modules (in arbitrary case).
 * That means we need to be careful if/when renaming or deleting any enum value or its name.
 */
enum class ComposePlatforms(vararg val alternativeNames: String) {
    KotlinMultiplatform("Common"),
    Desktop("Jvm"),
    AndroidDebug("Android"),
    AndroidRelease("Android"),
    Js("Web"),
    WasmJs("Web"),
    MacosX64("Macos"),
    MacosArm64("Macos"),
    UikitX64("UiKit"), // TODO: Align with AOSP: rename to iOS
    UikitArm64("UiKit"), // TODO: Align with AOSP: rename to iOS
    UikitSimArm64("UiKit"), // TODO: Align with AOSP: rename to iOS
    IosX64("Ios"),
    IosArm64("Ios"),
    IosSimulatorArm64("Ios"),
    TvosArm64("TvOs"),
    TvosX64("TvOs"),
    TvosSimulatorArm64("TvOs"),
    WatchosArm64("WatchOs"),
    WatchosArm32("WatchOs"),
    WatchosX64("WatchOs"),
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
            ComposePlatforms.Desktop,
            ComposePlatforms.AndroidDebug,
            ComposePlatforms.AndroidRelease
        )

        val UI_KIT = EnumSet.of(
            ComposePlatforms.UikitX64,
            ComposePlatforms.UikitArm64,
            ComposePlatforms.UikitSimArm64
        )

        val IOS = EnumSet.of(
            ComposePlatforms.IosX64,
            ComposePlatforms.IosArm64,
            ComposePlatforms.IosSimulatorArm64
        )

        val ANDROID = EnumSet.of(
            ComposePlatforms.AndroidDebug,
            ComposePlatforms.AndroidRelease
        )

        val LINUX = EnumSet.of(
            ComposePlatforms.LinuxX64,
            ComposePlatforms.LinuxArm64
        )

        val MACOS = EnumSet.of(
            ComposePlatforms.MacosX64,
            ComposePlatforms.MacosArm64
        )

        val WEB = EnumSet.of(
            ComposePlatforms.Js,
            ComposePlatforms.WasmJs
        )

        val SKIKO_SUPPORT = EnumSet.of(KotlinMultiplatform) + JVM_BASED + UI_KIT + MACOS + WEB

        val ALL = EnumSet.allOf(ComposePlatforms::class.java) - IOS
        val ALL_AOSP = EnumSet.of(KotlinMultiplatform) + JVM_BASED + IOS + LINUX + MACOS + WEB

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
