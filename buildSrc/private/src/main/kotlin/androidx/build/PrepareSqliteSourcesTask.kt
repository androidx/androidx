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

import javax.inject.Inject
import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.FileSystemOperations
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.process.ExecOperations
import org.gradle.work.DisableCachingByDefault

@DisableCachingByDefault(
    because = """
    In AndroidX-main, this is simply copying files, not worth caching.
    In Playground, we are downloading from remote git repo and we cannot know when it changed.
    For correctness on Github, it is a bit loose for now but we are using the `sqliteVersion`
    parameter to invalidate the task.
"""
)
abstract class PrepareSqliteSourcesTask @Inject constructor(
    private val execOperations: ExecOperations,
    private val fileSystemOperations: FileSystemOperations
) : DefaultTask() {
    @get:Input
    abstract val sqliteVersion: Property<String>

    @get:InputDirectory
    @get:PathSensitive(PathSensitivity.NONE)
    @get:Optional
    abstract val sqlitePrebuiltsDirectory: DirectoryProperty

    @get:OutputDirectory
    @get:Optional
    abstract val temporaryCheckoutDirectory: DirectoryProperty

    @get:OutputDirectory
    abstract val destinationDirectory: DirectoryProperty

    @TaskAction
    fun prepareSources() {
        val prebuilts = sqlitePrebuiltsDirectory.orNull?.asFile
        val checkoutDirectory = if (prebuilts != null) {
            check(prebuilts.exists()) {
                "Invalid prebuilts directory: $prebuilts"
            }
            prebuilts
        } else {
            val checkoutDir = temporaryCheckoutDirectory.orNull?.asFile
            check(checkoutDir != null) {
                "Checkout directory must be provided for playground builds"
            }
            // check it out
            val checkoutDirectory = temporaryCheckoutDirectory.asFile.get()
            checkoutDirectory.deleteRecursively()
            execOperations.exec {
                it.executable("git")
                it.args(
                    "clone",
                    SQLITE_REPO,
                    checkoutDirectory.canonicalPath, //clone into working dir
                    "--branch",
                    "androidx-main",
                    "--depth",
                    "1"
                )
            }
            checkoutDirectory
        }
        val sourceDirectory = destinationDirectory.asFile.get().also {
            it.deleteRecursively()
            it.mkdirs()
        }
        fileSystemOperations.copy {copySpec ->
            copySpec.from(checkoutDirectory.resolve("dist/orig"))
            copySpec.into(sourceDirectory)
            copySpec.include("sqlite3.c")
            copySpec.include("sqlite3.h")
        }
    }

    fun configureRemoteSources(project: Project) {
        if (ProjectLayoutType.isPlayground(project)) {
            temporaryCheckoutDirectory.set(
                project.layout.buildDirectory.dir("sqlite3/checkout")
            )
        } else {
            sqlitePrebuiltsDirectory.set(
                project.getExternalProjectPath().resolve(
                    "sqlite"
                )
            )
        }
    }

    companion object {
        private val SQLITE_REPO = "https://android.googlesource.com/platform/external/sqlite"
    }
}