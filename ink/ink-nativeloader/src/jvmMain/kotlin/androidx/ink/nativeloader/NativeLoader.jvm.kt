/*
 * Copyright (C) 2024 The Android Open Source Project
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

package androidx.ink.nativeloader

import androidx.annotation.RestrictTo
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.util.Locale
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Native code loader for Android and JVM.
 *
 * Supporting both in a single loader is helpful because Android code is run on JVM for Android host
 * tests.
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
actual public object NativeLoader {
    private var loaded = false
    private var mutex = Mutex()

    private val osName: String
        get() = System.getProperty("os.name")?.lowercase(Locale.US) ?: error("Cannot read osName")

    private val osArch: String
        get() = System.getProperty("os.arch")?.lowercase(Locale.US) ?: error("Cannot read osArch")

    private val platform: String
        get() =
            when {
                osName.contains("linux") && osArch == "amd64" -> "linux-x86_64"
                osName.contains("mac") && osArch == "aarch64" -> "macos-arm64"
                else -> error("Unsupported platform: $osName, $osArch")
            }

    actual public fun load() {
        // Fast bail-out before grabbing a lock if we don't need to.
        if (loaded) return
        runBlocking { mutex.withLock { loadSynchronous() } }
    }

    private fun loadSynchronous() {
        // Double-check in the synchronized block in case something got there after first check.
        if (loaded) return
        // On the JVM we need to find the correct libink library file in the JAR resources, copy it
        // out to a tempfile, and load it directly.
        //
        // See NativeLibraryLoader under sqlite/sqlite-bundled for a similar system.
        val tempFile = Files.createTempFile("libink.so", null).apply { toFile().deleteOnExit() }
        val resourcePath = "$platform/libink.so"
        checkNotNull(NativeLoader::class.java.classLoader!!.getResourceAsStream(resourcePath)) {
                "Could not find resource $resourcePath"
            }
            .use { resourceStream ->
                Files.copy(resourceStream, tempFile, StandardCopyOption.REPLACE_EXISTING)
            }
        System.load(tempFile.toFile().canonicalPath)
        loaded = true
    }
}
