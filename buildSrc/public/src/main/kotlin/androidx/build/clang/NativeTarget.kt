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

package androidx.build.clang

import androidx.build.clang.NativeTarget.Android
import java.io.Serializable
import org.jetbrains.kotlin.konan.target.Family
import org.jetbrains.kotlin.konan.target.KonanTarget

/**
 * Represents a target platform for native compilation.
 *
 * This class abstracts the compilation target, allowing us to support Android targets via the NDK
 * and other targets via Kotlin Native (Konan).
 */
sealed class NativeTarget(val name: String) : Serializable {
    abstract val family: Family

    val staticPrefix
        get() = family.staticPrefix

    val staticSuffix
        get() = family.staticSuffix

    val dynamicPrefix
        get() = family.dynamicPrefix

    val dynamicSuffix
        get() = family.dynamicSuffix

    /**
     * Returns the corresponding [KonanTarget] for non-Android targets. Throws an exception if
     * called on an Android target.
     */
    abstract fun toKonanTarget(): KonanTarget

    private class Android(name: String) : NativeTarget(name) {
        override val family = Family.ANDROID

        override fun toKonanTarget(): KonanTarget =
            error("Android targets do not have a corresponding KonanTarget")

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is Android) return false
            return name == other.name
        }

        override fun hashCode(): Int {
            return name.hashCode()
        }
    }

    private class Konan(val konanTarget: KonanTarget) : NativeTarget(konanTarget.name) {
        override val family = konanTarget.family

        override fun toKonanTarget() = konanTarget

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is Konan) return false
            return konanTarget == other.konanTarget
        }

        override fun hashCode(): Int {
            return konanTarget.hashCode()
        }
    }

    override fun toString(): String = name

    companion object {
        @JvmField val ANDROID_ARM32: NativeTarget = Android("android_arm32")
        @JvmField val ANDROID_ARM64: NativeTarget = Android("android_arm64")
        @JvmField val ANDROID_X86: NativeTarget = Android("android_x86")
        @JvmField val ANDROID_X64: NativeTarget = Android("android_x64")

        // Convenience constants for commonly used non-Android targets
        @JvmField val MACOS_ARM64: NativeTarget = Konan(KonanTarget.MACOS_ARM64)
        @JvmField val LINUX_X64: NativeTarget = Konan(KonanTarget.LINUX_X64)
        @JvmField val MINGW_X64: NativeTarget = Konan(KonanTarget.MINGW_X64)
        @JvmField val LINUX_ARM64: NativeTarget = Konan(KonanTarget.LINUX_ARM64)

        @JvmStatic
        fun fromName(name: String): NativeTarget {
            return when (name) {
                ANDROID_ARM32.name -> ANDROID_ARM32
                ANDROID_ARM64.name -> ANDROID_ARM64
                ANDROID_X86.name -> ANDROID_X86
                ANDROID_X64.name -> ANDROID_X64
                else -> {
                    val konanTarget =
                        KonanTarget.predefinedTargets[name] ?: error("Unknown native target: $name")
                    Konan(konanTarget)
                }
            }
        }
    }
}

val NativeTarget.isAndroid: Boolean
    get() = this.family == Family.ANDROID
