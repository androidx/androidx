/*
 * Copyright 2019 The Android Open Source Project
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

import java.io.FileInputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction

/** Task for verifying version files in Androidx artifacts */
@CacheableTask
abstract class VerifyVersionFilesTask : DefaultTask() {
    @get:[InputDirectory PathSensitive(PathSensitivity.RELATIVE)]
    abstract val repositoryDirectory: DirectoryProperty

    @TaskAction
    fun verifyVersionFilesPresent() {
        repositoryDirectory.asFile.get().walk().forEach { file ->
            var expectedPrefix = "androidx"
            if (file.path.contains("/libyuv/"))
                expectedPrefix = "libyuv_libyuv" // external library that we don't publish
            if (file.extension == "aar") {
                val inputStream = FileInputStream(file)
                val aarFileInputStream = ZipInputStream(inputStream)
                var entry: ZipEntry? = aarFileInputStream.nextEntry
                while (entry != null) {
                    if (entry.name == "classes.jar") {
                        var foundVersionFile = false
                        val classesJarInputStream = ZipInputStream(aarFileInputStream)
                        var jarEntry = classesJarInputStream.nextEntry
                        while (jarEntry != null) {
                            if (
                                jarEntry.name.startsWith("META-INF/$expectedPrefix.") &&
                                    jarEntry.name.endsWith(".version")
                            ) {
                                foundVersionFile = true
                                break
                            }
                            jarEntry = classesJarInputStream.nextEntry
                        }
                        if (!foundVersionFile) {
                            throw Exception(
                                "Missing classes.jar/META-INF/$expectedPrefix.*version " +
                                    "file in ${file.absolutePath}"
                            )
                        }
                        break
                    }
                    entry = aarFileInputStream.nextEntry
                }
            }
        }
    }
}
