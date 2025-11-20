/*
 * Copyright 2025 The Android Open Source Project
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

package androidx.sqlite.driver.test

import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.Locale
import kotlin.io.path.absolutePathString

/** Helper class to find native SQLite extension on the host platform. */
internal object LoadableExtension {
    /**
     * The name `sqliteExtension` is based on the native compilation name defined in the
     * `build.gradle` if it changes, so should this.
     *
     * TODO(b/433546325): Expose or generate named based of Gradle configuration
     */
    private const val LIB_NAME = "sqliteExtension"

    internal fun getExtensionFileName(): String {
        val osName =
            System.getProperty("os.name")?.lowercase(Locale.US) ?: error("Cannot read osName")
        val osPrefix =
            when {
                osName.contains("linux") -> "linux"
                osName.contains("mac") || osName.contains("osx") -> "osx"
                osName.contains("windows") -> "windows"
                else -> error("Unsupported operating system: $osName")
            }
        val osArch =
            System.getProperty("os.arch")?.lowercase(Locale.US) ?: error("Cannot read osArch")
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
        val libraryFileName =
            when (osPrefix) {
                "linux" -> "lib$LIB_NAME.so"
                "osx" -> "lib$LIB_NAME.dylib"
                "windows" -> "$LIB_NAME.dll"
                else -> error("Unsupported operating system: $osPrefix")
            }
        val tempFile = Files.createTempFile(libraryFileName, null).apply { toFile().deleteOnExit() }
        val resourceFileName = "natives/${osPrefix}_${archSuffix}/$libraryFileName"
        LoadableExtension::class.java.classLoader.getResourceAsStream(resourceFileName).use {
            resourceStream ->
            checkNotNull(resourceStream) {
                "Couldn't find native extension at '$resourceFileName'."
            }
            Files.copy(resourceStream, tempFile, StandardCopyOption.REPLACE_EXISTING)
        }
        return tempFile.absolutePathString()
    }
}
