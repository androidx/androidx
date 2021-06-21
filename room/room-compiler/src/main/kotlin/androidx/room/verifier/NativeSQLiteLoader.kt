/*
 * Copyright 2021 The Android Open Source Project
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

package androidx.room.verifier

import org.sqlite.SQLiteJDBCLoader
import org.sqlite.util.OSInfo
import java.io.File
import java.io.IOException
import java.io.InputStream
import java.util.UUID

/**
 * A custom sqlite-jdbc native library extractor and loader.
 *
 * This class is used instead of [SQLiteJDBCLoader.initialize] since it workarounds current issues
 * in the loading strategy, specifically: https://github.com/xerial/sqlite-jdbc/pull/578.
 */
internal object NativeSQLiteLoader {

    private var loaded = false

    private val tempDir: File by lazy {
        File(System.getProperty("org.sqlite.tmpdir", System.getProperty("java.io.tmpdir")))
    }

    private val version: String by lazy { SQLiteJDBCLoader.getVersion() }

    @JvmStatic
    fun load() = synchronized(loaded) {
        if (loaded) return
        try {
            // Cleanup target temporary folder for a new extraction.
            cleanupTempFolder()
            // Extract and load native library.
            loadNativeLibrary()
            // Reflect into original loader and mark library as extracted.
            SQLiteJDBCLoader::class.java.getDeclaredField("extracted")
                .apply { trySetAccessible() }
                .set(null, true)
        } catch (ex: Exception) {
            // Fallback to main library if our attempt failed, do print error juuust in case, so if
            // there is an error with our approach we get to know, instead of fully swallowing it.
            RuntimeException("Failed to load native SQLite library, will try again though.", ex)
                .printStackTrace()
            SQLiteJDBCLoader.initialize()
        }
        loaded = true
    }

    private fun cleanupTempFolder() {
        tempDir.listFiles { file ->
            file.name.startsWith("sqlite-$version") && !file.name.endsWith(".lck")
        }?.forEach { libFile ->
            val lckFile = File(libFile.absolutePath + ".lck")
            if (!lckFile.exists()) {
                libFile.delete()
            }
        }
    }

    // Load the OS-dependent library from the Jar file.
    private fun loadNativeLibrary() {
        val packagePath =
            SQLiteJDBCLoader::class.java.getPackage().name.replace(".", "/")
        val nativeLibraryPath =
            "/$packagePath/native/${OSInfo.getNativeLibFolderPathForCurrentOS()}"
        val nativeLibraryName = let {
            val libName = System.mapLibraryName("sqlitejdbc")
                .apply { replace("dylib", "jnilib") }
            if (hasResource("$nativeLibraryPath/$libName")) {
                return@let libName
            }
            if (OSInfo.getOSName() == "Mac") {
                // Fix for openjdk7 for Mac
                val altLibName = "libsqlitejdbc.jnilib"
                if (hasResource("$nativeLibraryPath/$altLibName")) {
                    return@let altLibName
                }
            }
            error(
                "No native library is found for os.name=${OSInfo.getOSName()} and " +
                    "os.arch=${OSInfo.getArchName()}. path=$nativeLibraryPath"
            )
        }

        val extractedNativeLibraryFile = try {
            extractNativeLibrary(nativeLibraryPath, nativeLibraryName, tempDir.absolutePath)
        } catch (ex: IOException) {
            throw RuntimeException("Couldn't extract native SQLite library.", ex)
        }
        try {
            @Suppress("UnsafeDynamicallyLoadedCode") // Loading an from an absolute path.
            System.load(extractedNativeLibraryFile.absolutePath)
        } catch (ex: UnsatisfiedLinkError) {
            throw RuntimeException("Couldn't load native SQLite library.", ex)
        }
    }

    private fun extractNativeLibrary(
        libraryPath: String,
        libraryName: String,
        targetDirPath: String
    ): File {
        val libraryFilePath = "$libraryPath/$libraryName"
        // Include arch name in temporary filename in order to avoid conflicts when multiple JVMs
        // with different architectures are running.
        val outputLibraryFile = File(
            targetDirPath,
            "sqlite-$version-${UUID.randomUUID()}-$libraryName"
        ).apply { deleteOnExit() }
        val outputLibraryLckFile = File(
            targetDirPath,
            "${outputLibraryFile.name}.lck"
        ).apply { deleteOnExit() }
        if (!outputLibraryLckFile.exists()) {
            outputLibraryLckFile.outputStream().close()
        }
        getResourceAsStream(libraryFilePath).use { inputStream ->
            outputLibraryFile.outputStream().use { outputStream ->
                inputStream.copyTo(outputStream)
            }
        }
        // Set executable flag (x) to enable loading the library.
        outputLibraryFile.setReadable(true)
        outputLibraryFile.setExecutable(true)
        return outputLibraryFile
    }

    private fun hasResource(path: String) = SQLiteJDBCLoader::class.java.getResource(path) != null

    // Replacement of java.lang.Class#getResourceAsStream(String) to disable sharing the resource
    // stream in multiple class loaders and specifically to avoid
    // https://bugs.openjdk.java.net/browse/JDK-8205976
    private fun getResourceAsStream(name: String): InputStream {
        // Remove leading '/' since all our resource paths include a leading directory
        // See: https://github.com/openjdk/jdk/blob/jdk-11+0/src/java.base/share/classes/java/lang/Class.java#L2573
        val resolvedName = name.drop(1)
        val url = SQLiteJDBCLoader::class.java.classLoader.getResource(resolvedName)
            ?: throw IOException("Resource '$resolvedName' could not be found.")
        return url.openConnection().apply {
            defaultUseCaches = false
        }.getInputStream()
    }
}