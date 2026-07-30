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

package androidx.build.clang

import androidx.build.OperatingSystem
import androidx.build.defaultAndroidConfig
import androidx.build.getSdkPath
import java.io.File
import java.util.Properties
import org.gradle.api.Project

/** Resolves the NDK directory. */
internal fun Project.getNdkDirectory(): File? {
    // Versioned NDK directory (sdkmanager install location, e.g. playground)
    val ndkVersion = defaultAndroidConfig.ndkVersion
    val versionedNdkDir = getSdkPath().resolve("ndk/$ndkVersion")
    if (versionedNdkDir.exists()) {
        return versionedNdkDir
    }
    // Bundled directory (repo checkout and prebuilts directory).
    val ndkBundleDir = getSdkPath().resolve("ndk-bundle")
    if (ndkBundleDir.exists()) {
        val sourcePropertiesFile = ndkBundleDir.resolve("source.properties")
        if (sourcePropertiesFile.exists()) {
            val properties =
                Properties().apply { sourcePropertiesFile.inputStream().use { load(it) } }
            val actualNdkVersion = properties.getProperty("Pkg.Revision")
            check(actualNdkVersion == ndkVersion) {
                "The NDK version in $sourcePropertiesFile ($actualNdkVersion) " +
                    "does not match the expected NDK version in gradle.properties ($ndkVersion)."
            }
        }
        return ndkBundleDir
    }
    return null
}

/** Gets the NDK clang executable. */
internal fun getNdkClangExecutable(
    ndkDir: File,
    target: NativeTarget,
    sdkLevel: Int,
    os: OperatingSystem,
    isCxx: Boolean,
): File {
    val prefix = getNdkClangPrefix(target)
    val suffix = if (isCxx) "++" else ""
    val binDir = ndkDir.resolve("toolchains/llvm/prebuilt/${getNdkHostPlatform(os)}/bin")
    return binDir.resolve("$prefix$sdkLevel-clang$suffix")
}

/** Gets the NDK archiver executable. */
internal fun getNdkLlvmArExecutable(ndkDir: File, os: OperatingSystem): File {
    val binDir = ndkDir.resolve("toolchains/llvm/prebuilt/${getNdkHostPlatform(os)}/bin")
    return binDir.resolve("llvm-ar")
}

private fun getNdkClangPrefix(target: NativeTarget): String {
    return when (target) {
        NativeTarget.ANDROID_ARM64 -> "aarch64-linux-android"
        NativeTarget.ANDROID_ARM32 -> "armv7a-linux-androideabi"
        NativeTarget.ANDROID_X64 -> "x86_64-linux-android"
        NativeTarget.ANDROID_X86 -> "i686-linux-android"
        else -> error("Unsupported NDK target: $target")
    }
}

private fun getNdkHostPlatform(os: OperatingSystem): String {
    return when (os) {
        OperatingSystem.LINUX -> "linux-x86_64"
        OperatingSystem.MAC -> "darwin-x86_64"
        OperatingSystem.WINDOWS -> error("NDK native compilation is not supported in Windows.")
    }
}
