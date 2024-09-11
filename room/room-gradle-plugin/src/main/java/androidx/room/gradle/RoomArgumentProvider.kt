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

import org.gradle.api.file.Directory
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Nested
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.process.CommandLineArgumentProvider

/**
 * Command line argument provider for annotation processing tasks to configure room-compiler and
 * wire the schema input directory as configured by the user using [RoomExtension.schemaDirectory]
 * and output directory (avoiding overlapping outputs) that will be an input of a
 * [RoomSchemaCopyTask].
 */
class RoomArgumentProvider(
    @get:Input val forKsp: Boolean,
    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    val schemaInputDir: Provider<Directory>,
    @get:OutputDirectory val schemaOutputDir: Provider<Directory>,
    @get:Nested val options: RoomOptions
) : CommandLineArgumentProvider {
    override fun asArguments() = buildList {
        val prefix = if (forKsp) "" else "-A"
        add("${prefix}room.internal.schemaInput=${schemaInputDir.get().asFile.path}")
        add("${prefix}room.internal.schemaOutput=${schemaOutputDir.get().asFile.path}")
        if (options.generateKotlin != null) {
            add("${prefix}room.generateKotlin=${options.generateKotlin}")
        }
    }
}

class RoomOptions(@Optional @get:Input val generateKotlin: Boolean?)

internal fun RoomExtension.toOptions(): RoomOptions {
    return RoomOptions(generateKotlin = this.generateKotlin)
}
