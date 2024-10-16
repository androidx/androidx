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

package androidx.sqlite.driver.bundled

import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.Locale

/** Helper class to load native libraries based on the host platform. */
internal actual object NativeLibraryLoader {
    // TODO(b/304281116): Generate this via Gradle so it is consistent.
    actual fun loadLibrary(name: String): Unit =
        synchronized(this) {
            try {
                System.loadLibrary(name)
                return
            } catch (error: UnsatisfiedLinkError) {
                // Likely not on Android, continue...
            }
            // TODO(b/304281116): Improve loading implementation
            val libResourceName = getResourceName(name)
            val libTempCopy =
                Files.createTempFile("androidx_$name", null).apply { toFile().deleteOnExit() }
            NativeLibraryLoader::class
                .java
                .classLoader!!
                .getResourceAsStream(libResourceName)
                .use { resourceStream ->
                    checkNotNull(resourceStream) {
                        "Cannot find a suitable SQLite binary for ${System.getProperty("os.name")} | " +
                            "${System.getProperty("os.arch")}. Please file a bug at " +
                            "https://issuetracker.google.com/issues/new?component=460784"
                    }
                    Files.copy(resourceStream, libTempCopy, StandardCopyOption.REPLACE_EXISTING)
                }
            @Suppress("UnsafeDynamicallyLoadedCode") System.load(libTempCopy.toFile().canonicalPath)
        }

    /** Gets the JAR's resource file path to the native library. */
    private fun getResourceName(name: String): String {
        val osName =
            System.getProperty("os.name")?.lowercase(Locale.US) ?: error("Cannot read osName")
        val osArch =
            System.getProperty("os.arch")?.lowercase(Locale.US) ?: error("Cannot read osArch")
        val osPrefix =
            when {
                osName.contains("linux") -> "linux"
                osName.contains("mac") || osName.contains("osx") -> "osx"
                osName.contains("windows") -> "windows"
                else -> error("Unsupported operating system: $osName")
            }
        val archSuffix =
            when {
                osArch == "aarch64" -> "arm64"
                osArch.contains("arm") ->
                    when {
                        osArch.contains("64") -> "arm64"
                        else -> "arm32"
                    }
                osArch.contains("64") -> "x64"
                osArch.contains("86") -> "x86"
                else -> error("Unsupported architecture: $osArch")
            }
        val resourceFolder = "${osPrefix}_$archSuffix"
        val prefix =
            when (osPrefix) {
                "linux",
                "osx" -> "lib"
                "windows" -> ""
                else -> error("Unsupported operating system: $osName")
            }
        val extension =
            when (osPrefix) {
                "linux" -> "so"
                "osx" -> "dylib"
                "windows" -> "dll"
                else -> error("Unsupported operating system: $osName")
            }
        return "natives/$resourceFolder/$prefix$name.$extension"
    }
}
