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
import androidx.build.getSdkPath
import androidx.build.getNdkVersion
import java.io.File
import org.gradle.api.Project

/** Resolves the NDK directory. */
internal fun Project.getNdkDirectory(): File? {
    // Try versioned NDK directory first (playground and sdkmanager install location)
    val ndkVersion = getNdkVersion()
    val versionedNdkDir = getSdkPath().resolve("ndk/$ndkVersion")
    if (versionedNdkDir.exists()) {
        return versionedNdkDir
    }
    // Try bundled directory as fallback, repo checkout with prebuilt.
    val ndkBundleDir = getSdkPath().resolve("ndk-bundle")
    return if (ndkBundleDir.exists()) ndkBundleDir else null
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
