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

package androidx.build

import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.MapProperty
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskAction

@CacheableTask
abstract class AttestationManifestTask : DefaultTask() {
    @get:Input abstract val sbomMap: MapProperty<String, String>

    @get:Input abstract val zipMap: MapProperty<String, String>

    @get:OutputFile abstract val manifestFile: RegularFileProperty

    @TaskAction
    fun writeManifest() {
        val output =
            zipMap.get().keys.joinToString(separator = ",\n", prefix = "[\n", postfix = "\n]") { key
                ->
                check(sbomMap.get().containsKey(key)) {
                    "sbomMap is missing an entry for $key project"
                }
                """  {
    "artifact_path": "${zipMap.get()[key]!!}",
    "sbom_path": "${sbomMap.get()[key]!!}",
    "attest_archive_contents": true
  }"""
            }
        manifestFile.get().asFile.writeText(output)
    }
}

internal const val ATTESTATION_TASK_NAME = "attestationManifest"
