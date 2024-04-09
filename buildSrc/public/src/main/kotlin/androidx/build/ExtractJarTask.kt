/*
 * Copyright 2024 The Android Open Source Project
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

package androidx.build

import java.io.File
import java.util.zip.ZipFile
import org.gradle.api.DefaultTask
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import org.gradle.work.DisableCachingByDefault

/**
 * A task designed to extract the contents of one or more JAR files. This task accepts a collection
 * of JAR files as input and extracts their contents into a specified output directory.
 * Each JAR file is processed individually, and its contents are placed directly into the output
 * directory, maintaining the internal structure of the JAR files.
 */
@DisableCachingByDefault
abstract class ExtractJarTask : DefaultTask() {

    @get:InputFiles
    abstract val jarFiles: ConfigurableFileCollection

    @get:OutputDirectory
    abstract val outputDir: DirectoryProperty

    @TaskAction
    fun extractJars() {
        val outputDirectory = outputDir.get().asFile
        if (!outputDirectory.exists()) {
            outputDirectory.mkdirs()
        }

        jarFiles.forEach { jarFile ->
            ZipFile(jarFile).use { zip ->
                zip.entries().asSequence().forEach { entry ->
                    val outputFile = File(outputDirectory, entry.name)
                    if (!entry.isDirectory) {
                        outputFile.parentFile.mkdirs()
                        zip.getInputStream(entry).use { input ->
                            outputFile.outputStream().use { output ->
                                input.copyTo(output)
                            }
                        }
                    }
                }
            }
        }
    }
}
