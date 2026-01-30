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

package androidx.inspection.gradle

import java.io.FileOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import javax.inject.Inject
import org.gradle.api.DefaultTask
import org.gradle.api.file.ArchiveOperations
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.FileSystemOperations
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.work.DisableCachingByDefault

/** Task to facilitate repackaging an AAR with the inspector.jar. */
@DisableCachingByDefault(because = "Not worth caching")
abstract class AddInspectorJarToAarTask : DefaultTask() {

    @get:Inject abstract val fs: FileSystemOperations

    @get:Inject abstract val archiveOperations: ArchiveOperations

    @get:InputFile
    @get:PathSensitive(PathSensitivity.NONE)
    abstract val inputAar: RegularFileProperty

    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val inspectorJar: ConfigurableFileCollection

    @get:OutputFile abstract val outputAar: RegularFileProperty

    @TaskAction
    fun repackage() {
        val tempUnpackDir = temporaryDir
        fs.delete { it.delete(tempUnpackDir) }
        tempUnpackDir.mkdirs()

        fs.copy {
            it.from(archiveOperations.zipTree(inputAar.get().asFile))
            it.into(tempUnpackDir)
        }

        fs.copy {
            it.from(inspectorJar.singleFile)
            it.rename { "inspector.jar" }
            it.into(tempUnpackDir)
        }

        val outputAarFile = outputAar.get().asFile
        ZipOutputStream(FileOutputStream(outputAarFile)).use { zipOut ->
            tempUnpackDir.walkTopDown().forEach { fileOrDir ->
                if (fileOrDir == tempUnpackDir) return@forEach

                val relativePath = fileOrDir.relativeTo(tempUnpackDir).invariantSeparatorsPath
                val entryName = if (fileOrDir.isDirectory) "$relativePath/" else relativePath

                zipOut.putNextEntry(ZipEntry(entryName))
                if (fileOrDir.isFile) {
                    fileOrDir.inputStream().use { it.copyTo(zipOut) }
                }
                zipOut.closeEntry()
            }
        }
    }
}
