/*
 * Copyright 2026 The Android Open Source Project
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

package androidx.annotation.keep

import java.util.zip.ZipInputStream
import org.gradle.api.DefaultTask
import org.gradle.api.file.Directory
import org.gradle.api.file.RegularFile
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Classpath
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction
import org.objectweb.asm.ClassReader

@CacheableTask
abstract class ExtractKeepRulesTask : DefaultTask() {

    @get:Classpath abstract val inputDirectories: ListProperty<Directory>

    @get:Classpath abstract val inputJars: ListProperty<RegularFile>

    @get:OutputFile abstract val outputKeepRules: RegularFileProperty

    @TaskAction
    fun extractRules() {
        val rulesAccumulator = StringBuilder()

        for (directory in inputDirectories.get()) {
            val dirFile = directory.asFile
            if (dirFile.exists() && dirFile.isDirectory) {
                dirFile
                    .walkTopDown()
                    .filter { it.isFile && it.name.endsWith(".class") }
                    .forEach { classFile ->
                        val reader = ClassReader(classFile.readBytes())
                        val visitor =
                            KeepAnnoStub.createClassVisitorForKeepRulesExtraction { rule ->
                                rulesAccumulator.append(rule)
                            }
                        reader.accept(
                            visitor,
                            ClassReader.SKIP_CODE or
                                ClassReader.SKIP_DEBUG or
                                ClassReader.SKIP_FRAMES,
                        )
                    }
            }
        }

        for (jar in inputJars.get()) {
            val jarFile = jar.asFile
            if (jarFile.exists() && jarFile.isFile) {
                ZipInputStream(jarFile.inputStream().buffered()).use { zipInputStream ->
                    var entry = zipInputStream.nextEntry
                    while (entry != null) {
                        if (entry.name.endsWith(".class")) {
                            val reader = ClassReader(zipInputStream.readBytes())
                            val visitor =
                                KeepAnnoStub.createClassVisitorForKeepRulesExtraction { rule ->
                                    rulesAccumulator.append(rule)
                                }
                            reader.accept(
                                visitor,
                                ClassReader.SKIP_CODE or
                                    ClassReader.SKIP_DEBUG or
                                    ClassReader.SKIP_FRAMES,
                            )
                        }
                        entry = zipInputStream.nextEntry
                    }
                }
            }
        }

        val outputFile = outputKeepRules.get().asFile
        outputFile.parentFile.mkdirs()
        outputFile.writeText(rulesAccumulator.toString())
    }
}
