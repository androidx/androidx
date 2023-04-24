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

package androidx.stableaidl.tasks

import java.io.File
import java.nio.file.Files
import org.apache.tools.zip.ZipEntry
import org.apache.tools.zip.ZipFile
import org.apache.tools.zip.ZipOutputStream
import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.FileTree
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.work.DisableCachingByDefault

/**
 * Transforms an AAR by adding parcelable headers.
 */
@DisableCachingByDefault(because = "Primarily filesystem operations")
abstract class StableAidlPackageApi : DefaultTask() {
    @get:InputFile
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val aarFile: RegularFileProperty

    @get:InputDirectory
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val packagedDir: DirectoryProperty

    @get:OutputFile
    abstract val updatedAarFile: RegularFileProperty

    @TaskAction
    fun taskAction() {
        aidlPackageApiDelegate(
            aarFile.get().asFile,
            updatedAarFile.get().asFile,
            packagedDir.get().asFileTree,
            name
        )
    }
}

internal fun aidlPackageApiDelegate(
    aar: File,
    updatedAar: File,
    packagedTree: FileTree,
    name: String,
) {
    val tempDir = Files.createTempDirectory("${name}Unzip").toFile()
    tempDir.deleteOnExit()

    ZipFile(aar).use { aarFile ->
        aarFile.unzipTo(tempDir)
    }

    val aidlRoot = File(tempDir, "aidl")
    if (!aidlRoot.exists()) {
        aidlRoot.mkdir()
    }

    // Copy the directory structure and files.
    packagedTree.visit { details ->
        val target = File(aidlRoot, details.relativePath.pathString)
        if (details.isDirectory) {
            target.mkdir()
        } else {
            details.copyTo(target)
        }
    }

    tempDir.zipTo(updatedAar)
    tempDir.deleteRecursively()
}

internal fun ZipFile.unzipTo(tempDir: File) {
    entries.iterator().forEach { entry ->
        if (entry.isDirectory) {
            File(tempDir, entry.name).mkdirs()
        } else {
            val file = File(tempDir, entry.name)
            file.parentFile.mkdirs()
            getInputStream(entry).use { stream ->
                file.writeBytes(stream.readBytes())
            }
        }
    }
}

internal fun File.zipTo(outZip: File) {
    ZipOutputStream(outZip.outputStream()).use { stream ->
        listFiles()!!.forEach { file ->
            stream.addFileRecursive(null, file)
        }
    }
}

internal fun ZipOutputStream.addFileRecursive(parentPath: String?, file: File) {
    val entryPath = if (parentPath != null) "$parentPath/${file.name}" else file.name
    val entry = ZipEntry(file, entryPath)

    // Reset creation time of entry to make it deterministic.
    entry.time = 0
    entry.creationTime = java.nio.file.attribute.FileTime.fromMillis(0)

    if (file.isFile) {
        putNextEntry(entry)
        file.inputStream().use { stream ->
            stream.copyTo(this)
        }
        closeEntry()
    } else if (file.isDirectory) {
        val listFiles = file.listFiles()
        if (!listFiles.isNullOrEmpty()) {
            putNextEntry(entry)
            closeEntry()
            listFiles.forEach { containedFile ->
                addFileRecursive(entryPath, containedFile)
            }
        }
    }
}
