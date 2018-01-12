/*
 * Copyright 2018 The Android Open Source Project
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

package android.arch.navigation.safeargs.gradle

import android.arch.navigation.safe.args.generator.generateSafeArgs
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import java.io.File

open class ArgumentsGenerationTask : DefaultTask() {

    @get:Input
    var applicationId: String = ""

    @get:OutputDirectory
    var outputDir: File? = null

    @get:InputFiles
    var navigationFiles: List<File> = emptyList()

    @TaskAction
    fun perform() {
        val out = outputDir ?: throw GradleException("ArgumentsGenerationTask must have outputDir")
        navigationFiles.forEach { file ->
            generateSafeArgs(applicationId, file, out)
        }
    }
}