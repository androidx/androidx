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

package androidx.sqliteMultiplatform.unbundled

import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.Locale

internal actual object NativeLibraryLoader {
    // TODO(b/304281116): Generate this via Gradle so it is consistent.
    actual fun loadLibrary(name: String) {
        try {
            System.loadLibrary(name)
            return
        } catch (error: Throwable) {
            // looks like we are not on Android, continue
        }
        // TODO(b/304281116): Temporary loading implementation
        val osName =
            System.getProperty("os.name")?.lowercase(Locale.US) ?: error("Cannot read osName")
        val osArch =
            System.getProperty("os.arch")?.lowercase(Locale.US) ?: error("Cannot read osArch")
        val osPrefix = when {
            osName.contains("linux") -> "linux"
            osName.contains("mac") || osName.contains("osx") -> "osx"
            else -> error("Unsupported operating system: $osName")
        }
        val archSuffix = when {
            osArch == "aarch64" -> "arm64"
            osArch.contains("arm") -> when {
                osArch.contains("64") -> "arm64"
                else -> "arm32"
            }
            osArch.contains("64") -> "x64"
            osArch.contains("86") -> "x86"
            else -> error("Unsupported architecture: $osArch")
        }
        val resourceFolder = "${osPrefix}_$archSuffix"
        val ext = if (osPrefix == "linux") { "so" } else { "dylib" }
        val resourceName = "$resourceFolder/lib$name.$ext"
        val nativeLibCopy = Files.createTempFile("androidx_$name", null)
        nativeLibCopy.toFile().deleteOnExit()
        NativeLibraryLoader::class.java.classLoader!!.getResourceAsStream(
            resourceName
        ).use { resourceStream ->
            checkNotNull(resourceStream) {
                "Cannot find resource $resourceName"
            }
            Files.copy(resourceStream, nativeLibCopy, StandardCopyOption.REPLACE_EXISTING)
        }
        @Suppress("UnsafeDynamicallyLoadedCode")
        System.load(nativeLibCopy.toFile().canonicalPath)
    }
}
