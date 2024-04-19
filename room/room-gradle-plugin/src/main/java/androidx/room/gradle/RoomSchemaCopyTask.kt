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

package androidx.room.gradle

import java.io.File
import java.security.MessageDigest
import kotlin.io.path.copyTo
import kotlin.io.path.createDirectories
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.tasks.IgnoreEmptyDirectories
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.SkipWhenEmpty
import org.gradle.api.tasks.TaskAction
import org.gradle.work.DisableCachingByDefault

@DisableCachingByDefault(because = "Simple disk bound task.")
abstract class RoomSchemaCopyTask : DefaultTask() {
    @get:InputFiles
    @get:SkipWhenEmpty
    @get:IgnoreEmptyDirectories
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val variantSchemaOutputDirectories: ConfigurableFileCollection

    @get:Internal
    abstract val schemaDirectory: DirectoryProperty

    @TaskAction
    fun copySchemas() {
        // Map of relative path to its source file hash.
        val copiedHashes = mutableMapOf<String, MutableMap<String, String>>()
        variantSchemaOutputDirectories.files
            .filter { it.exists() }
            .forEach { outputDir ->
                outputDir.walkTopDown().filter { it.isFile }.forEach { schemaFile ->
                    val schemaPath = schemaFile.toPath()
                    val basePath = outputDir.toPath().relativize(schemaPath)
                    val target = schemaDirectory.get().asFile.toPath().resolve(basePath)
                        .apply { parent?.createDirectories() }
                    schemaPath.copyTo(target = target, overwrite = true)
                    copiedHashes.getOrPut(basePath.toString()) { mutableMapOf() }
                        .put(schemaFile.sha256(), schemaPath.toString())
                }
            }
        // Validate that if multiple schema files for the same database and version are copied
        // to the same schema directory that they are the same in content (via checksum), as
        // otherwise it would indicate per-variant schemas and thus requiring per-variant
        // schema directories.
        copiedHashes.filterValues { it.size > 1 }.forEach { (_, hashes) ->
            val errorMsg = buildString {
                appendLine(
                    "Inconsistency detected exporting Room schema files (checksum - source):"
                )
                hashes.entries.forEach {
                    appendLine("  ${it.key} - ${it.value}")
                }
                appendLine(
                    "The listed files differ in content but were copied into the same " +
                        "schema directory '${schemaDirectory.get()}'. A possible indicator that " +
                        "per-variant / per-target schema locations must be provided."
                )
            }
            throw GradleException(errorMsg)
        }
    }

    private fun File.sha256(): String {
        return MessageDigest.getInstance("SHA-256").digest(this.readBytes())
            .joinToString("") { "%02x".format(it) }
    }
}
