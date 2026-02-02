/*
 * Copyright 2021 The Android Open Source Project
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
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.work.DisableCachingByDefault

@DisableCachingByDefault(because = "Doesn't benefit from cache")
abstract class SingleFileCopy : DefaultTask() {
    @get:InputFile
    @get:PathSensitive(PathSensitivity.ABSOLUTE)
    abstract val sourceFile: RegularFileProperty

    @get:OutputFile abstract val destinationFile: RegularFileProperty

    @TaskAction
    fun copyFile() {
        val source = sourceFile.get().asFile
        val destination = destinationFile.get().asFile
        destination.parentFile.mkdirs()
        source.copyTo(destination, overwrite = true)
    }
}
