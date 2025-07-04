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

import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.Locale

/** Helper class to load native libraries based on the host platform. */
internal actual object NativeLibraryLoader {

    private const val LIB_PATH_PROPERTY_NAME = "androidx.sqlite.driver.bundled.path"

    private val osName: String
        get() = System.getProperty("os.name")?.lowercase(Locale.US) ?: error("Cannot read osName")

    private val osArch: String
        get() = System.getProperty("os.arch")?.lowercase(Locale.US) ?: error("Cannot read osArch")

    private val osPrefix: String
        get() =
            when {
                osName.contains("linux") -> "linux"
                osName.contains("mac") || osName.contains("osx") -> "osx"
                osName.contains("windows") -> "windows"
                else -> error("Unsupported operating system: $osName")
            }

    private val archSuffix: String
        get() =
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

    // TODO(b/304281116): Generate this via Gradle so it is consistent.
    actual fun loadLibrary(name: String): Unit =
        synchronized(this) {
            // Load for Android
            try {
                System.loadLibrary(name)
                return
            } catch (_: UnsatisfiedLinkError) {
                // Likely not on Android, continue...
            }

            val libName = getLibraryName(name)

            // Load from configured property path
            val libraryPath = System.getProperty(LIB_PATH_PROPERTY_NAME)
            if (libraryPath != null) {
                val libFile = File("$libraryPath/$libName")
                check(libFile.exists()) {
                    "Cannot find a suitable SQLite binary for $osName | $osArch at the " +
                        "configured path ($LIB_PATH_PROPERTY_NAME = $libraryPath). " +
                        "File $libFile does not exist."
                }
                tryLoad(libFile.canonicalPath)
                return
            }

            // Load from shared lib/ or bin/ dir in Java home
            val javaHomeLibs =
                File(System.getProperty("java.home"), if (osPrefix == "windows") "bin" else "lib")
            val libFile = javaHomeLibs.resolve(libName)
            if (libFile.exists()) {
                tryLoad(libFile.canonicalPath)
                return
            }

            // Load from temp file extracted from resources
            val libResourceName = getResourceName(libName)
            val libTempCopy =
                Files.createTempFile("androidx_$name", null).apply { toFile().deleteOnExit() }
            NativeLibraryLoader::class
                .java
                .classLoader!!
                .getResourceAsStream(libResourceName)
                .use { resourceStream ->
                    checkNotNull(resourceStream) {
                        "Cannot find a suitable SQLite binary for $osName | $osArch. " +
                            "Please file a bug at " +
                            "https://issuetracker.google.com/issues/new?component=460784"
                    }
                    Files.copy(resourceStream, libTempCopy, StandardCopyOption.REPLACE_EXISTING)
                }
            tryLoad(libTempCopy.toFile().canonicalPath)
        }

    /** Gets the native library file name. */
    private fun getLibraryName(name: String): String {
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
        return "$prefix$name.$extension"
    }

    /** Gets the JAR's resource file path to the native library. */
    private fun getResourceName(libName: String): String {
        val resourceFolder = "${osPrefix}_$archSuffix"
        return "natives/$resourceFolder/$libName"
    }

    private fun tryLoad(path: String) {
        @Suppress("UnsafeDynamicallyLoadedCode") System.load(path)
    }
}
