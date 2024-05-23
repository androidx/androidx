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

package androidx.build.clang

import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.work.DisableCachingByDefault

/**
 * Creates a CInterop def file based on an [original] with added static library path to include the
 * given [objectFile].
 *
 * Once KT-62800 is fixed, we can consider removing this task and do all of this programmatically.
 *
 * https://kotlinlang.org/docs/native-c-interop.html
 */
@DisableCachingByDefault(because = "not worth caching, it is a copy with file modification")
abstract class CreateDefFileWithLibraryPathTask : DefaultTask() {
    @get:InputFile
    @get:PathSensitive(PathSensitivity.NONE)
    abstract val original: RegularFileProperty

    @get:InputFile
    @get:PathSensitive(PathSensitivity.NAME_ONLY)
    abstract val objectFile: RegularFileProperty

    @get:OutputFile abstract val target: RegularFileProperty

    @get:Internal abstract val projectDir: DirectoryProperty

    @TaskAction
    fun createPlatformSpecificDefFile() {
        val target = target.asFile.get()
        target.parentFile?.mkdirs()
        // use relative path to the owning project so it can be cached (as much as possible).
        // Right now,the only way to add libraryPaths/staticLibraries is the def file, which
        // resolves paths relative to the project. Once KT-62800 is fixed, we should remove this
        // task but until than, this is the only option to pass a generated so file
        val objectFileParentDir =
            objectFile.asFile
                .get()
                .parentFile
                .canonicalFile
                .relativeTo(projectDir.get().asFile.canonicalFile)
        val outputContents =
            listOf(
                    original.asFile.get().readText(Charsets.UTF_8),
                    "libraryPaths=\"$objectFileParentDir\"",
                    "staticLibraries=" + objectFile.asFile.get().name
                )
                .joinToString(System.lineSeparator())
        target.writeText(outputContents, Charsets.UTF_8)
    }
}
