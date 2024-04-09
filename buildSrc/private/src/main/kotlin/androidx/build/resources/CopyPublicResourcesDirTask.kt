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

package androidx.build.resources

import java.io.File
import javax.inject.Inject
import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.FileSystemOperations
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.work.DisableCachingByDefault

/**
 * Copy task that adds a [DirectoryProperty] to be used in variant.addGeneratedSourceDirectory()
 */
@DisableCachingByDefault(
    because = " Copy tasks are faster to rerun locally than to fetch from the remote cache."
)
abstract class CopyPublicResourcesDirTask : DefaultTask() {

    @get:Inject
    abstract val fileSystemOperations: FileSystemOperations

    @get:InputDirectory
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val buildSrcResDir: DirectoryProperty

    @get:OutputDirectory
    abstract val outputFolder: DirectoryProperty

    @TaskAction
    fun copy() {
        File(outputFolder.get().asFile.path).apply {
            deleteRecursively()
            mkdirs()
            fileSystemOperations.copy {
                it.from(buildSrcResDir)
                it.into(this)
            }
        }
    }
}
