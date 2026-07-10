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

import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.SkipWhenEmpty
import org.gradle.api.tasks.TaskAction

/**
 * Task for verifying the ELF regions in all shared libs in androidx are aligned to 16Kb boundary
 */
@CacheableTask
abstract class VerifyELFRegionAlignmentTask : DefaultTask() {
    init {
        group = "Verification"
        description = "Task for verifying alignment in shared libs"
    }

    @get:[InputDirectory PathSensitive(PathSensitivity.RELATIVE) SkipWhenEmpty]
    abstract val mergedNativeLibs: DirectoryProperty

    @TaskAction
    fun verifyELFRegionAlignment() {
        val prebuiltLibraries = listOf("libtracing_perfetto.so", "libc++_shared.so")
        mergedNativeLibs
            .get()
            .asFileTree
            .files
            .filter { it.extension == "so" }
            // Android 15 introduces support for 16KB page sizes:
            // https://developer.android.com/guide/practices/page-sizes
            // To be compatible with these devices, native libraries on arm64-v8a must be aligned to
            // 16KB boundaries (2**14).
            .filter { it.path.contains("arm64-v8a") }
            .filterNot { prebuiltLibraries.contains(it.name) }
            .forEach {
                val alignment = getELFAlignment(it.path)
                check(alignment == "2**14") {
                    "Expected ELF alignment of 2**14 for file ${it.name}, got $alignment"
                }
            }
    }
}

private fun getELFAlignment(filePath: String): String? {
    val alignment =
        ProcessBuilder("objdump", "-p", filePath).start().inputStream.bufferedReader().useLines {
            lines ->
            lines.filter { it.contains("LOAD") }.map { it.split(" ").last() }.firstOrNull()
        }
    return alignment
}
