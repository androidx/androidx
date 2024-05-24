/*
 * Copyright 2022 The Android Open Source Project
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

package androidx.benchmark.darwin.gradle

import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.Serializable
import java.net.URI
import java.util.zip.ZipInputStream
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFile
import org.gradle.api.provider.Property
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction

/** Fetches the `xcodegen` binary which is used to build the XCode project. */
@CacheableTask
abstract class FetchXCodeGenTask : DefaultTask() {
    @get:Input abstract val xcodeGenUri: Property<String>

    @get:OutputDirectory abstract val downloadPath: DirectoryProperty

    @TaskAction
    fun fetchXcodeGenTask() {
        val uri = URI(xcodeGenUri.get())
        when (uri.scheme) {
            "https" -> {
                downloadAndExtractXcodeGen(uri)
            }
            "file" -> {
                copyXcodeGen(File(uri))
            }
            else -> throw GradleException("Unsupported scheme")
        }
    }

    private fun downloadAndExtractXcodeGen(uri: URI) {
        val downloadRoot = downloadPath.get().asFile
        // Setup download location
        downloadRoot.deleteRecursively()
        downloadRoot.mkdirs()
        val url = uri.toURL()
        val inputStream = url.openStream()
        // Download
        val zipFile = File(downloadRoot, "xcodegen.zip")
        inputStream.use {
            val outputStream = zipFile.outputStream()
            outputStream.use { inputStream.copyTo(it) }
        }
        // Setup Output Location
        // This must end with a `bin` for the xcodegen path discovery to work
        val xcodeGenBinaryPath = File(downloadRoot, "bin")
        xcodeGenBinaryPath.mkdirs()
        // Extract
        val zipStream = ZipInputStream(FileInputStream(zipFile))
        zipStream.use {
            var entry = zipStream.nextEntry
            while (entry != null) {
                if (entry.name.endsWith("/bin/xcodegen")) {
                    val output = FileOutputStream(File(xcodeGenBinaryPath, "xcodegen"))
                    output.use { zipStream.copyTo(output) }
                    break
                } else {
                    zipStream.closeEntry()
                    entry = zipStream.nextEntry
                }
            }
        }
        markExecutable()
    }

    private fun copyXcodeGen(source: File) {
        val downloadRoot = downloadPath.get().asFile
        source.copyRecursively(downloadRoot, overwrite = true)
        markExecutable()
    }

    private fun findXcodeGen(): File {
        val downloadRoot = downloadPath.get().asFile
        return downloadRoot.walkTopDown().asSequence().toList().find { file ->
            file.absolutePath.endsWith("/bin/xcodegen")
        } ?: throw GradleException("Cannot find `xcodegen` binary in $downloadRoot")
    }

    private fun markExecutable() {
        val xcodeGenBinary = findXcodeGen()
        require(xcodeGenBinary.setExecutable(true)) { "Cannot mark $xcodeGenBinary executable." }
    }

    fun xcodeGenBinary(): RegularFile {
        return LocalRegularFile(findXcodeGen())
    }

    companion object {
        class LocalRegularFile(private val file: File) : RegularFile, Serializable {
            override fun getAsFile(): File {
                return file.absoluteFile
            }
        }
    }
}
