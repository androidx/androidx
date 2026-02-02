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
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Classpath
import org.gradle.api.tasks.InputFiles
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

    @get:[InputFiles Classpath]
    abstract val files: ConfigurableFileCollection

    @TaskAction
    fun verifyELFRegionAlignment() {
        files.forEach {
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
